package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsLookupService;

import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionsEventProcessor {
    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final PdsLookupService pdsLookupService;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;

    public void processSuspensionEvent(String suspensionMessage) {
        String nhsNumber = extractNhsNumber(suspensionMessage);
        String previousOdsCode=extractPreviousOdsCode(suspensionMessage);
        PdsAdaptorSuspensionStatusResponse response = pdsLookupService.isSuspended(nhsNumber);
        String recordATag=response.getRecordETag();

        if(Boolean.TRUE.equals(response.getIsSuspended())){
            mofUpdatedEventPublisher.sendMessage(suspensionMessage);
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
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
        HashMap<String, Object> map = new HashMap<String,Object>();
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
