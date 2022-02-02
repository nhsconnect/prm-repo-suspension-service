package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
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
    private final PdsService pdsService;

    @Value("${process_only_synthetic_patients}")
    private String processOnlySyntheticPatients;

    @Value("${synthetic_patient_prefix}")
    private String syntheticPatientPrefix;

    private final ObjectMapper mapper = new ObjectMapper();
    private final SuspensionEventParser parser;

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
        SuspensionEvent suspensionEvent = parser.parse(suspensionMessage);
        PdsAdaptorSuspensionStatusResponse response;
        try {
            response = getPdsAdaptorSuspensionStatusResponse(suspensionEvent);
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            invalidSuspensionPublisher.sendMessage(suspensionMessage);
            throw invalidPdsRequestException;
        }

        if (processingOnlySyntheticPatients() && patientIsNonSynthetic(suspensionEvent)) {
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
            return suspensionMessage;
        }

        if (Boolean.TRUE.equals(response.getIsSuspended())) {
            log.info("Patient is Suspended");
            try {
                updateMof(response.getNhsNumber(), response.getRecordETag(), response.getManagingOrganisation(), suspensionMessage, suspensionEvent);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            } catch (InvalidPdsRequestException invalidPdsRequestException){
                invalidSuspensionPublisher.sendMessage(suspensionMessage);
                throw invalidPdsRequestException;
            }
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
        return suspensionMessage;
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
        if (!suspensionEvent.nhsNumber().equals(response.getNhsNumber())) {
            log.info("Processing a superseded record");
            var supersededNhsNumber = response.getNhsNumber();
            response = pdsService.isSuspended(supersededNhsNumber);
        }
        return response;
    }

    private void updateMof(String nhsNumber,
                           String recordETag,
                           String newManagingOrganisation,
                           String suspensionMessage,
                           SuspensionEvent suspensionEvent) throws JsonProcessingException {
        if (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode())) {
            var updateMofResponse = pdsService.updateMof(nhsNumber, suspensionEvent.previousOdsCode(), recordETag);
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            publishMofUpdateMessage(nhsNumber, updateMofResponse, suspensionEvent.nemsMessageId());
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void publishMofUpdateMessage(String nhsNumber,
                                         PdsAdaptorSuspensionStatusResponse updateMofResponse,
                                         String nemsMessageId) {
        var managingOrganisationPublisherMessage = new ManagingOrganisationUpdatedMessage(
                nhsNumber,
                updateMofResponse.getManagingOrganisation(),
                nemsMessageId);
        try {
            mofUpdatedEventPublisher.sendMessage(mapper.writeValueAsString(managingOrganisationPublisherMessage));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
