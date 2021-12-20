package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        if(pdsLookupService.isSuspended(nhsNumber).getIsSuspended()){
            mofUpdatedEventPublisher.sendMessage(suspensionMessage);
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private String extractNhsNumber(String suspensionMessage) {
        HashMap<String, Object> map = mapMessageToHashMap(suspensionMessage);

        return  validateNhsNumber(map);
    }

    private HashMap<String, Object> mapMessageToHashMap(String suspensionMessage) {
        HashMap<String, Object> map = new HashMap<String,Object>();
        final ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(suspensionMessage, new TypeReference<HashMap<String,Object>>(){});
        } catch (JsonProcessingException e) {
            log.error("Message is not message");
        }
        return map;
    }

    private String validateNhsNumber(HashMap<String, Object> map) {
        if(map.get("nhsNumber")==null){
            log.error("Nhs number is not proper");
            throw new IllegalArgumentException("Nhs number is not proper");
        }
        String nhsNumber = map.get("nhsNumber").toString();
        if (!nhsNumber.matches("[0-9]+") && nhsNumber.length() != 10){
            log.error("Nhs number is not proper");
            throw new IllegalArgumentException("Nhs number is not proper");
        }
        return nhsNumber;
    }
}
