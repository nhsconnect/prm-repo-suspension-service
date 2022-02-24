package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.db.EventOutOfDateService;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProcessExecution {
    final NotSuspendedEventPublisher notSuspendedEventPublisher;
    final MofUpdatedEventPublisher mofUpdatedEventPublisher;
    final MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    final InvalidSuspensionPublisher invalidSuspensionPublisher;
    final EventOutOfDatePublisher eventOutOfDatePublisher;
    final PdsService pdsService;
    final EventOutOfDateService eventOutOfDateService;
    @Value("${process_only_synthetic_patients}")
    String processOnlySyntheticPatients;
    @Value("${synthetic_patient_prefix}")
    String syntheticPatientPrefix;
    final SuspensionEventParser parser;
    final ConcurrentThreadLock threadLock;

    public String run(String suspensionMessage) {
        var suspensionEvent = getSuspensionEvent(suspensionMessage);
        try {
            threadLock.lock(suspensionEvent.nhsNumber());

            // synthetic patient block
            if (processingOnlySyntheticPatients() && patientIsNonSynthetic(suspensionEvent)) {
                var notSyntheticMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:NOT_SYNTHETIC");
                mofNotUpdatedEventPublisher.sendMessage(notSyntheticMessage);
                return suspensionMessage;
            }

            // event out of date block
            if (eventOutOfDateService.checkIfEventIsOutOfDate(suspensionEvent.nhsNumber(), suspensionEvent.lastUpdated())) {
                var eventOutOfDateMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:EVENT_PROCESSED_OUT_OF_ORDER");
                eventOutOfDatePublisher.sendMessage(eventOutOfDateMessage);
                return suspensionMessage;
            }

            // pds adaptor block
            PdsAdaptorSuspensionStatusResponse response;
            try {
                response = getPdsAdaptorSuspensionStatusResponse(suspensionEvent);
            } catch (InvalidPdsRequestException invalidPdsRequestException) {
                return publishInvalidSuspension(suspensionMessage, suspensionEvent.nemsMessageId(), invalidPdsRequestException);
            }

            if (Boolean.TRUE.equals(response.getIsSuspended())) {
                log.info("Patient is Suspended");
                publishMofUpdate(suspensionMessage, suspensionEvent, response);
            } else {
                var notSuspendedMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");
                notSuspendedEventPublisher.sendMessage(notSuspendedMessage);
            }
        } finally {
            threadLock.unlock(suspensionEvent.nhsNumber());
        }

        return suspensionMessage;
    }

    SuspensionEvent getSuspensionEvent(String suspensionMessage) {
        SuspensionEvent suspensionEvent;
        try {
            suspensionEvent = parser.parse(suspensionMessage);
        } catch (InvalidSuspensionMessageException exception) {
            invalidSuspensionPublisher.sendMessage(suspensionMessage);
            invalidSuspensionPublisher.sendNonSensitiveMessage(suspensionMessage);
            throw new InvalidSuspensionMessageException("Encountered an invalid message", exception);
        }
        return suspensionEvent;
    }

    void publishMofUpdate(String suspensionMessage, SuspensionEvent suspensionEvent, PdsAdaptorSuspensionStatusResponse response) {
        try {
            updateMof(response.getNhsNumber(), response.getRecordETag(), response.getManagingOrganisation(), suspensionEvent);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            publishInvalidSuspension(suspensionMessage, suspensionEvent.nemsMessageId(), invalidPdsRequestException);
        }
    }

    String publishInvalidSuspension(String suspensionMessage, String nemsMessageId, InvalidPdsRequestException invalidPdsRequestException) {
        invalidSuspensionPublisher.sendMessage(suspensionMessage);
        invalidSuspensionPublisher.sendNonSensitiveMessage(new NonSensitiveDataMessage(nemsMessageId,
                "NO_ACTION:INVALID_SUSPENSION").toJsonString());

        throw invalidPdsRequestException;
    }

    boolean patientIsNonSynthetic(SuspensionEvent suspensionEvent) {
        boolean isNonSynthetic = !suspensionEvent.nhsNumber().startsWith(syntheticPatientPrefix);
        log.info(isNonSynthetic ? "Processing Non-Synthetic Patient" : "Processing Synthetic Patient");
        return isNonSynthetic;
    }

    boolean processingOnlySyntheticPatients() {
        log.info("Process only synthetic patients: " + processOnlySyntheticPatients);
        return Boolean.parseBoolean(processOnlySyntheticPatients);
    }

    PdsAdaptorSuspensionStatusResponse getPdsAdaptorSuspensionStatusResponse(SuspensionEvent suspensionEvent) {
        log.info("Checking patient's suspension status on PDS");
        PdsAdaptorSuspensionStatusResponse response = pdsService.isSuspended(suspensionEvent.nhsNumber());
        if (nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), response.getNhsNumber())) {
            log.info("Processing a superseded record");
            var supersededNhsNumber = response.getNhsNumber();
            response = pdsService.isSuspended(supersededNhsNumber);
        }
        return response;
    }

    boolean nhsNumberIsSuperseded(String nemsEventNhsNumber, String pdsNhsNumber) {
        return !nemsEventNhsNumber.equals(pdsNhsNumber);
    }

    void updateMof(String nhsNumber,
                   String recordETag,
                   String newManagingOrganisation,
                   SuspensionEvent suspensionEvent) throws JsonProcessingException {
        if (canUpdateManagingOrganisation(newManagingOrganisation, suspensionEvent)) {
            var updateMofResponse = pdsService.updateMof(nhsNumber, suspensionEvent.previousOdsCode(), recordETag);
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            publishMofUpdateMessage(nhsNumber, suspensionEvent);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            var mofSameAsPreviousGp = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:MOF_SAME_AS_PREVIOUS_GP");
            mofNotUpdatedEventPublisher.sendMessage(mofSameAsPreviousGp);
        }
    }

    boolean canUpdateManagingOrganisation(String newManagingOrganisation, SuspensionEvent suspensionEvent) {
        return (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode()));
    }

    void publishMofUpdateMessage(String pdsNhsNumber, SuspensionEvent suspensionEvent) {
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(suspensionEvent.nemsMessageId(), suspensionEvent.previousOdsCode(), "ACTION:UPDATED_MANAGING_ORGANISATION");
        if (nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), pdsNhsNumber)) {
            mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(suspensionEvent.nemsMessageId(), suspensionEvent.previousOdsCode(), "ACTION:UPDATED_MANAGING_ORGANISATION_FOR_SUPERSEDED_PATIENT");
        }
        mofUpdatedEventPublisher.sendMessage(mofUpdatedMessage);
    }
}