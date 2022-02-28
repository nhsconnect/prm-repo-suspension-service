package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.data.LastUpdatedEventService;
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
    final DeceasedPatientEventPublisher deceasedPatientEventPublisher;
    final PdsService pdsService;
    final LastUpdatedEventService lastUpdatedEventService;
    @Value("${process_only_synthetic_patients}")
    String processOnlySyntheticPatients;
    @Value("${synthetic_patient_prefix}")
    String syntheticPatientPrefix;
    final SuspensionEventParser parser;
    final ConcurrentThreadLock threadLock;

    public void run(String suspensionMessage) {
        var suspensionEvent = getSuspensionEvent(suspensionMessage);
        try {
            threadLock.lock(suspensionEvent.nhsNumber());

            // synthetic patient block
            if (processingOnlySyntheticPatients() && patientIsNonSynthetic(suspensionEvent)) {
                var notSyntheticMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:NOT_SYNTHETIC");
                mofNotUpdatedEventPublisher.sendMessage(notSyntheticMessage);
                return;
            }

            // event out of date block
            if (lastUpdatedEventService.isOutOfDate(suspensionEvent.nhsNumber(), suspensionEvent.lastUpdated())) {
                log.info("Event is out of date");
                var eventOutOfDateMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:EVENT_PROCESSED_OUT_OF_ORDER");
                eventOutOfDatePublisher.sendMessage(eventOutOfDateMessage);
                return;
            }

            // pds adaptor block
            var pdsAdaptorSuspensionStatusResponse = getPdsAdaptorSuspensionStatusResponse(suspensionMessage, suspensionEvent);

            // patient is deceased block
            if (Boolean.TRUE.equals(pdsAdaptorSuspensionStatusResponse.getIsDeceased())) {
                log.info("Patient is deceased");
                var deceasedPatientMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:DECEASED_PATIENT");
                deceasedPatientEventPublisher.sendMessage(deceasedPatientMessage);
                return;
            }

            if (Boolean.TRUE.equals(pdsAdaptorSuspensionStatusResponse.getIsSuspended())) {
                log.info("Patient is Suspended");
                publishMofUpdate(suspensionMessage, suspensionEvent, pdsAdaptorSuspensionStatusResponse);
                lastUpdatedEventService.save(suspensionEvent.nhsNumber(), suspensionEvent.lastUpdated());
            } else {
                var notSuspendedMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");
                notSuspendedEventPublisher.sendMessage(notSuspendedMessage);
            }
        } finally {
            threadLock.unlock(suspensionEvent.nhsNumber());
        }
    }

    void publishMofUpdate(String suspensionMessage, SuspensionEvent suspensionEvent, PdsAdaptorSuspensionStatusResponse response) {
        try {
            updateMof(response.getNhsNumber(), response.getRecordETag(), response.getManagingOrganisation(), suspensionEvent);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            publishInvalidSuspensionAndThrow(suspensionMessage, suspensionEvent.nemsMessageId(), invalidPdsRequestException);
        }
    }

    PdsAdaptorSuspensionStatusResponse getPdsAdaptorSuspensionStatusResponse(String suspensionMessage, SuspensionEvent suspensionEvent) {
        try {
            log.info("Checking patient's suspension status on PDS");
            var response = pdsService.isSuspended(suspensionEvent.nhsNumber());
            if (nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), response.getNhsNumber())) {
                log.info("Processing a superseded record");
                var supersededNhsNumber = response.getNhsNumber();
                response = pdsService.isSuspended(supersededNhsNumber);
            }
            return response;
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            publishInvalidSuspensionAndThrow(suspensionMessage, suspensionEvent.nemsMessageId(), invalidPdsRequestException);
            return null;  // publishInvalidSuspensionAndThrow throws, so this line is never reached
        }
    }

    String publishInvalidSuspensionAndThrow(String suspensionMessage, String nemsMessageId, InvalidPdsRequestException invalidPdsRequestException) {
        invalidSuspensionPublisher.sendMessage(suspensionMessage);
        invalidSuspensionPublisher.sendNonSensitiveMessage(new NonSensitiveDataMessage(nemsMessageId,
                "NO_ACTION:INVALID_SUSPENSION").toJsonString());
        throw invalidPdsRequestException;
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

    boolean patientIsNonSynthetic(SuspensionEvent suspensionEvent) {
        boolean isNonSynthetic = !suspensionEvent.nhsNumber().startsWith(syntheticPatientPrefix);
        log.info(isNonSynthetic ? "Processing Non-Synthetic Patient" : "Processing Synthetic Patient");
        return isNonSynthetic;
    }

    boolean processingOnlySyntheticPatients() {
        log.info("Process only synthetic patients: " + processOnlySyntheticPatients);
        return Boolean.parseBoolean(processOnlySyntheticPatients);
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