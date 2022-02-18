package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@Slf4j
public class SuspensionEventParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public SuspensionEvent parse(String suspensionMessage) {
        return new SuspensionEvent(parseIntoMap(suspensionMessage));
    }

    private HashMap<String, Object> parseIntoMap(String suspensionMessage) {
        HashMap<String, Object> map;
        try {
            map = mapper.readValue(suspensionMessage, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing suspensions message to a map.");
            throw new InvalidSuspensionMessageException("Failed to parse Suspension Message", e);
        }
        return map;
    }
}
