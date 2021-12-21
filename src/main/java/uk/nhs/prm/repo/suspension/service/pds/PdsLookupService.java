package uk.nhs.prm.repo.suspension.service.pds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;

import java.util.Arrays;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class PdsLookupService {

    private RestTemplate pdsAdaptorClient;

    private static final String SUSPENDED_PATIENT = "suspended-patient-status/";

    public PdsAdaptorSuspensionStatusResponse isSuspended(String nhsNumber) {
        ResponseEntity<String> responseEntity = pdsAdaptorClient
                .exchange(SUSPENDED_PATIENT + nhsNumber, HttpMethod.GET, prepareHeader(), String.class);

        ObjectMapper mapper = new ObjectMapper();
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse = null;
        try {
            Map<String, Object> map = mapper.readValue(responseEntity.getBody(), Map.class);
            pdsAdaptorSuspensionStatusResponse = new PdsAdaptorSuspensionStatusResponse(Boolean.valueOf(map.get("isSuspended").toString()),
                    map.get("currentOdsCode").toString());
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing PDS lookup response.");
        }
        return pdsAdaptorSuspensionStatusResponse;
    }

    private HttpEntity<String> prepareHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<String>(headers);

        return entity;
    }
}