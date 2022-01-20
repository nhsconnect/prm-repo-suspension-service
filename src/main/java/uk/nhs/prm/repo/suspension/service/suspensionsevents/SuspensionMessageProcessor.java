package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationPublisherMessage;
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
        SuspensionEvent suspensionEvent = parser.parse(suspensionMessage, this);
        PdsAdaptorSuspensionStatusResponse response = getPdsAdaptorSuspensionStatusResponse(suspensionEvent);
        if (Boolean.parseBoolean(processOnlySyntheticPatients)){
            if(!suspensionEvent.nhsNumber().startsWith(syntheticPatientPrefix)){
                return;
            }
        }

        if (Boolean.TRUE.equals(response.getIsSuspended())){
            try {
                updateMof(response.getNhsNumber(), response.getRecordETag(), response.getManagingOrganisation(), suspensionMessage, suspensionEvent);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private PdsAdaptorSuspensionStatusResponse getPdsAdaptorSuspensionStatusResponse(SuspensionEvent suspensionEvent) {
        PdsAdaptorSuspensionStatusResponse response = pdsService.isSuspended(suspensionEvent.nhsNumber());
        if (!suspensionEvent.nhsNumber().equals(response.getNhsNumber())) {
            log.info("Processing a superseded record");
            var supersededNhsNumber = response.getNhsNumber();
            response = pdsService.isSuspended(supersededNhsNumber);
        }
        return response;
    }

    private void updateMof(String nhsNumber, String recordETag, String newManagingOrganisation, String suspensionMessage, SuspensionEvent suspensionEvent) throws JsonProcessingException {
        if (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode())) {
            PdsAdaptorSuspensionStatusResponse updateMofResponse = pdsService.updateMof(nhsNumber, suspensionEvent.previousOdsCode(), recordETag);
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            publishMofUpdateMessage(nhsNumber, updateMofResponse);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void publishMofUpdateMessage(String nhsNumber, PdsAdaptorSuspensionStatusResponse updateMofResponse) {
        ManagingOrganisationPublisherMessage managingOrganisationPublisherMessage = new ManagingOrganisationPublisherMessage(nhsNumber,
                updateMofResponse.getManagingOrganisation());
        try {
            mofUpdatedEventPublisher.sendMessage(mapper.writeValueAsString(managingOrganisationPublisherMessage));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

}