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
    private final MessagePublisherBroker messagePublisherBroker;
    private final PdsService pdsService;
    private final LastUpdatedEventService lastUpdatedEventService;
    private final ManagingOrganisationService managingOrganisationService;

    @Value("${process_only_synthetic_patients}")
    private String processOnlySyntheticPatients;

    @Value("${synthetic_patient_prefix}")
    private String syntheticPatientPrefix;

    private final SuspensionEventParser parser;
    private final ConcurrentThreadLock threadLock;

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
                managingOrganisationService.processMofUpdate(suspensionMessage, suspensionEvent, pdsAdaptorSuspensionStatusResponse);
                lastUpdatedEventService.save(suspensionEvent.nhsNumber(), suspensionEvent.lastUpdated());
            } else {
                messagePublisherBroker.notSuspendedMessage(suspensionEvent.nemsMessageId());
            }
        } finally {
            threadLock.unlock(suspensionEvent.nhsNumber());
        }
    }

    private PdsAdaptorSuspensionStatusResponse getPdsAdaptorSuspensionStatusResponse(String suspensionMessage, SuspensionEvent suspensionEvent) {
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
            messagePublisherBroker.invalidFormattedMessage(suspensionMessage, new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:INVALID_SUSPENSION").toJsonString());
            throw invalidPdsRequestException;
        }
    }

    private SuspensionEvent getSuspensionEvent(String suspensionMessage) {
        try {
            return parser.parse(suspensionMessage);
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing suspensions message");
            messagePublisherBroker.invalidFormattedMessage(suspensionMessage, suspensionMessage);
            throw new InvalidSuspensionMessageException("Encountered an invalid message", e);
        }
    }

    private boolean patientIsNonSynthetic(SuspensionEvent suspensionEvent) {
        boolean isNonSynthetic = !suspensionEvent.nhsNumber().startsWith(syntheticPatientPrefix);
        log.info(isNonSynthetic ? "Processing Non-Synthetic Patient" : "Processing Synthetic Patient");
        return isNonSynthetic;
    }

    private boolean processingOnlySyntheticPatients() {
        log.info("Process only synthetic patients: " + processOnlySyntheticPatients);
        return Boolean.parseBoolean(processOnlySyntheticPatients);
    }

    private boolean nhsNumberIsSuperseded(String nemsEventNhsNumber, String pdsNhsNumber) {
        return !nemsEventNhsNumber.equals(pdsNhsNumber);
    }

}