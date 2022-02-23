package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.db.EventOutOfDateService;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.IntermittentErrorPdsException;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionMessageProcessor {
    @Value("${suspension.processor.retry.max.attempts}")
    private int maxAttempts;

    @Value("${suspension.processor.initial.interval.millisecond}")
    private int initialIntervalMillis;

    @Value("${suspension.processor.initial.interval.multiplier}")
    private double multiplier;

    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;
    private final MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    private final InvalidSuspensionPublisher invalidSuspensionPublisher;
    private final EventOutOfDatePublisher eventOutOfDatePublisher;
    private final PdsService pdsService;
    private final EventOutOfDateService eventOutOfDateService;

    @Value("${process_only_synthetic_patients}")
    private String processOnlySyntheticPatients;

    @Value("${synthetic_patient_prefix}")
    private String syntheticPatientPrefix;

    private final SuspensionEventParser parser;

    private final ConcurrentThreadLock threadLock;

    public void processSuspensionEvent(String message) {
        Function<String, String> retryableProcessEvent = Retry
                .decorateFunction(Retry.of("retryableSuspension", createRetryConfig()), this::processSuspensionEventOnce);
        retryableProcessEvent.apply(message);
    }

    private RetryConfig createRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialIntervalMillis, multiplier))
                .retryExceptions(IntermittentErrorPdsException.class)
                .build();
    }

    private String processSuspensionEventOnce(String suspensionMessage) {
        SuspensionEvent suspensionEvent;
        suspensionEvent = getSuspensionEvent(suspensionMessage);
        try {
            PdsAdaptorSuspensionStatusResponse response;

            if (eventOutOfDateService.checkIfEventIsOutOfDate(suspensionEvent.nhsNumber(), suspensionEvent.lastUpdated())) {
                var eventOutOfDateMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:EVENT_PROCESSED_OUT_OF_ORDER");
                eventOutOfDatePublisher.sendMessage(eventOutOfDateMessage);
                return suspensionMessage;
            }

            threadLock.lock(suspensionEvent.nhsNumber());

            try {
                response = getPdsAdaptorSuspensionStatusResponse(suspensionEvent);
            } catch (InvalidPdsRequestException invalidPdsRequestException) {
                return publishInvalidSuspension(suspensionMessage, suspensionEvent.nemsMessageId(), invalidPdsRequestException);
            }

            if (processingOnlySyntheticPatients() && patientIsNonSynthetic(suspensionEvent)) {
                var notSyntheticMessage = new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(), "NO_ACTION:NOT_SYNTHETIC");
                mofNotUpdatedEventPublisher.sendMessage(notSyntheticMessage);
                return suspensionMessage;
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

    private SuspensionEvent getSuspensionEvent(String suspensionMessage) {
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

    private void publishMofUpdate(String suspensionMessage, SuspensionEvent suspensionEvent, PdsAdaptorSuspensionStatusResponse response) {
        try {
            updateMof(response.getNhsNumber(), response.getRecordETag(), response.getManagingOrganisation(), suspensionEvent);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            publishInvalidSuspension(suspensionMessage, suspensionEvent.nemsMessageId(), invalidPdsRequestException);
        }
    }

    private String publishInvalidSuspension(String suspensionMessage, String nemsMessageId, InvalidPdsRequestException invalidPdsRequestException) {
        invalidSuspensionPublisher.sendMessage(suspensionMessage);
        invalidSuspensionPublisher.sendNonSensitiveMessage(new NonSensitiveDataMessage(nemsMessageId,
                "NO_ACTION:INVALID_SUSPENSION").toJsonString());

        throw invalidPdsRequestException;
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

    private PdsAdaptorSuspensionStatusResponse getPdsAdaptorSuspensionStatusResponse(SuspensionEvent suspensionEvent) {
        log.info("Checking patient's suspension status on PDS");
        PdsAdaptorSuspensionStatusResponse response = pdsService.isSuspended(suspensionEvent.nhsNumber());
        if (nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), response.getNhsNumber())) {
            log.info("Processing a superseded record");
            var supersededNhsNumber = response.getNhsNumber();
            response = pdsService.isSuspended(supersededNhsNumber);
        }
        return response;
    }

    private boolean nhsNumberIsSuperseded(String nemsEventNhsNumber, String pdsNhsNumber) {
        return !nemsEventNhsNumber.equals(pdsNhsNumber);
    }

    private void updateMof(String nhsNumber,
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

    private boolean canUpdateManagingOrganisation(String newManagingOrganisation, SuspensionEvent suspensionEvent) {
        return (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode()));
    }

    private void publishMofUpdateMessage(String pdsNhsNumber, SuspensionEvent suspensionEvent) {
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(suspensionEvent.nemsMessageId(), suspensionEvent.previousOdsCode(), "ACTION:UPDATED_MANAGING_ORGANISATION");
        if (nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), pdsNhsNumber)) {
            mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(suspensionEvent.nemsMessageId(), suspensionEvent.previousOdsCode(), "ACTION:UPDATED_MANAGING_ORGANISATION_FOR_SUPERSEDED_PATIENT");
        }
        mofUpdatedEventPublisher.sendMessage(mofUpdatedMessage);
    }

}
