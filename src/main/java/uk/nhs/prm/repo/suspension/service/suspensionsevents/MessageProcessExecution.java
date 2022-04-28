package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.data.LastUpdatedEventService;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;
import uk.nhs.prm.repo.suspension.service.publishers.MessagePublisherBroker;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProcessExecution {
    final MessagePublisherBroker messagePublisherBroker;
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

            // event out of order block
            if (lastUpdatedEventService.isOutOfOrder(suspensionEvent.nhsNumber(), suspensionEvent.lastUpdated())) {
                log.info("Event is out of order");
                messagePublisherBroker.eventOutOfOrderMessage(suspensionEvent.nemsMessageId());
                return;
            }

            // pds adaptor block
            var pdsAdaptorSuspensionStatusResponse = getPdsAdaptorSuspensionStatusResponse(suspensionMessage, suspensionEvent);

            // patient is deceased block
            if (Boolean.TRUE.equals(pdsAdaptorSuspensionStatusResponse.getIsDeceased())) {
                log.info("Patient is deceased");
                messagePublisherBroker.deceasedPatientMessage(suspensionEvent.nemsMessageId());
                return;
            }

            // synthetic patient block
            if (processingOnlySyntheticPatients() && patientIsNonSynthetic(suspensionEvent)) {
                messagePublisherBroker.notSyntheticMessage(suspensionEvent.nemsMessageId());
                return;
            }

            if (Boolean.TRUE.equals(pdsAdaptorSuspensionStatusResponse.getIsSuspended())) {
                log.info("Patient is Suspended");
                publishMofUpdate(suspensionMessage, suspensionEvent, pdsAdaptorSuspensionStatusResponse);
                lastUpdatedEventService.save(suspensionEvent.nhsNumber(), suspensionEvent.lastUpdated());
            } else {
                messagePublisherBroker.notSuspendedMessage(suspensionEvent.nemsMessageId());
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
        messagePublisherBroker.invalidFormattedMessage(suspensionMessage, new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:INVALID_SUSPENSION").toJsonString());
        throw invalidPdsRequestException;
    }

    SuspensionEvent getSuspensionEvent(String suspensionMessage) {
        try {
            return parser.parse(suspensionMessage);
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing suspensions message");
            messagePublisherBroker.invalidFormattedMessage(suspensionMessage, suspensionMessage);
            throw new InvalidSuspensionMessageException("Encountered an invalid message", e);
        }
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
            var isSuperseded = nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), nhsNumber);
            messagePublisherBroker.mofUpdatedMessage(suspensionEvent.nemsMessageId(), suspensionEvent.previousOdsCode() , isSuperseded);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            messagePublisherBroker.mofNotUpdatedMessage(suspensionEvent.nemsMessageId());
        }
    }

    boolean canUpdateManagingOrganisation(String newManagingOrganisation, SuspensionEvent suspensionEvent) {
        return (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode()));
    }

}