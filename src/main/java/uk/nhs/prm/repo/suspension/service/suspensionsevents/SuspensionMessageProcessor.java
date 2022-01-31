package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionMessageProcessor {
    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;
    private final MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    private final PdsService pdsService;

    @Value("${process_only_synthetic_patients}")
    private String processOnlySyntheticPatients;

    @Value("${synthetic_patient_prefix}")
    private String syntheticPatientPrefix;

    private final ObjectMapper mapper = new ObjectMapper();
    private final SuspensionEventParser parser;

    public void processSuspensionEvent(String suspensionMessage) {
        SuspensionEvent suspensionEvent = parser.parse(suspensionMessage);
        PdsAdaptorSuspensionStatusResponse response = getPdsAdaptorSuspensionStatusResponse(suspensionEvent);

        if (processingOnlySyntheticPatients() && patientIsNonSynthetic(suspensionEvent)) {
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
            return;
        }

        if (Boolean.TRUE.equals(response.getIsSuspended())){
            log.info("Patient is Suspended");
            try {
                updateMof(response.getNhsNumber(), response.getRecordETag(), response.getManagingOrganisation(), suspensionMessage, suspensionEvent);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
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
