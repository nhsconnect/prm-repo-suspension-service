package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationPublisherMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionsEventProcessor {
    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;
    private final MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    private final PdsService pdsService;
    private final ObjectMapper mapper;

    public void processSuspensionEvent(String suspensionMessage) {
        String nhsNumber = extractNhsNumber(suspensionMessage);
        PdsAdaptorSuspensionStatusResponse response = pdsService.isSuspended(nhsNumber);

        if (Boolean.TRUE.equals(response.getIsSuspended())){
            try {
                updateMof(nhsNumber, response.getRecordETag(), response.getManagingOrganisation(), suspensionMessage);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void updateMof(String nhsNumber, String recordETag, Object managingOrganisation, String suspensionMessage) throws JsonProcessingException {
        String previousOdsCode = extractPreviousOdsCode(suspensionMessage);
        if (!managingOrganisation.toString().equals(previousOdsCode)) {
            PdsAdaptorSuspensionStatusResponse updateMofResponse = pdsService.updateMof(nhsNumber, previousOdsCode, recordETag);
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            publishMofUpdateMessage(nhsNumber, updateMofResponse);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void publishMofUpdateMessage(String nhsNumber, PdsAdaptorSuspensionStatusResponse updateMofResponse) {
        ManagingOrganisationPublisherMessage managingOrganisationPublisherMessage = new ManagingOrganisationPublisherMessage(nhsNumber,
                updateMofResponse.getManagingOrganisation().toString());
        try {
            mofUpdatedEventPublisher.sendMessage(mapper.writeValueAsString(managingOrganisationPublisherMessage));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    private String extractNhsNumber(String suspensionMessage) {
        HashMap<String, Object> map = mapMessageToHashMap(suspensionMessage);

        return map.get("nhsNumber").toString();
    }

    private String extractPreviousOdsCode(String suspensionMessage){
        HashMap<String, Object> map = mapMessageToHashMap(suspensionMessage);
        return map.get("previousOdsCode").toString();
    }


    private HashMap<String, Object> mapMessageToHashMap(String suspensionMessage) {
        HashMap<String, Object> map = new HashMap<>();
        try {
            map = mapper.readValue(suspensionMessage, new TypeReference<HashMap<String,Object>>(){});
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing suspensions message to a map.");
        }
        return map;
    }

}
