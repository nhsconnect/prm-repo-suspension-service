package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsLookupService;
import uk.nhs.prm.repo.suspension.service.pds.PdsUpdateService;

import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionsEventProcessor {
    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final PdsLookupService pdsLookupService;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;
    private final MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    private final PdsUpdateService pdsUpdateService;

    public void processSuspensionEvent(String suspensionMessage) {
        String nhsNumber = extractNhsNumber(suspensionMessage);
        PdsAdaptorSuspensionStatusResponse response = pdsLookupService.isSuspended(nhsNumber);

        if (Boolean.TRUE.equals(response.getIsSuspended())){
            updateMof(nhsNumber, response.getRecordETag(), response.getManagingOrganisation(), suspensionMessage);
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void updateMof(String nhsNumber, String recordETag, Object managingOrganisation, String suspensionMessage) {
        String previousOdsCode = extractPreviousOdsCode(suspensionMessage);
        if (!managingOrganisation.toString().equals(previousOdsCode)) {
            PdsAdaptorSuspensionStatusResponse updateMofResponse = pdsUpdateService.updateMof(nhsNumber, previousOdsCode, recordETag);
            log.info("MOF Updated to " + updateMofResponse.getManagingOrganisation());
            mofUpdatedEventPublisher.sendMessage(suspensionMessage);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private String extractNhsNumber(String suspensionMessage) {
        HashMap<String, Object> map = mapMessageToHashMap(suspensionMessage);

        return  validateNhsNumber(map);
    }

    private String extractPreviousOdsCode(String suspensionMessage){
        HashMap<String, Object> map = mapMessageToHashMap(suspensionMessage);
        return validateOdsCode(map);
    }


    private HashMap<String, Object> mapMessageToHashMap(String suspensionMessage) {
        HashMap<String, Object> map = new HashMap<>();
        final ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(suspensionMessage, new TypeReference<HashMap<String,Object>>(){});
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing suspensions message to a map.");
        }
        return map;
    }

    private String validateNhsNumber(HashMap<String, Object> map) {
        if(map.get("nhsNumber")==null){
            log.error("Nhs number is not proper");
            throw new IllegalArgumentException("Message has no nhs number.");
        }
        String nhsNumber = map.get("nhsNumber").toString();
        if (!nhsNumber.matches("[0-9]+") && nhsNumber.length() != 10){
            log.error("Nhs number is invalid.");
            throw new IllegalArgumentException("Nhs number is invalid.");
        }
        return nhsNumber;
    }

    private String validateOdsCode(HashMap<String, Object> map) {
        if(map.get("previousOdsCode")==null){
            log.error("Ods can not be null");
            throw new IllegalArgumentException("Ods can not be null");
        }
        return map.get("previousOdsCode").toString();
    }
}
