package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationPublisherMessage;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SuspensionsEventParser {

    private final ObjectMapper mapper;

    public SuspensionsEventParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String parseMofUpdateMessage(ManagingOrganisationPublisherMessage managingOrganisationPublisherMessage) throws JsonProcessingException {
        return mapper.writeValueAsString(managingOrganisationPublisherMessage);
    }

    public Map<String, Object> mapMessageToHashMap(String suspensionMessage) {
        Map<String, Object> map = new HashMap<>();
        try {
            map = mapper.readValue(suspensionMessage, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing suspensions message to a map.");
        }
        return map;
    }
}
