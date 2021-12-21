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

@Component
@AllArgsConstructor
@Slf4j
public class PdsLookupService {

    public static final String SUSPENSION_SERVICE_USERNAME = "suspension-service";
    private RestTemplate pdsAdaptorClient;

    private static final String SUSPENDED_PATIENT = "suspended-patient-status/";

    @Value("${pdsAdaptor.suspensionService.password}")
    private String suspensionServicePassword;

    @Autowired
    public PdsLookupService(RestTemplate pdsAdaptorClient) {
        this.pdsAdaptorClient = pdsAdaptorClient;
    }

    public PdsAdaptorSuspensionStatusResponse isSuspended(String nhsNumber) {
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = pdsAdaptorClient
                    .exchange(SUSPENDED_PATIENT + nhsNumber, HttpMethod.GET, prepareHeader(), String.class);
        } catch (Exception e) {
            //log message is publishing as an info log, this is just to make it visible.
            log.error(e.getMessage());
        }
        ObjectMapper mapper = new ObjectMapper();
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse = null;
        try {
            pdsAdaptorSuspensionStatusResponse = mapper.readValue(responseEntity.getBody(), PdsAdaptorSuspensionStatusResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Got an exception while parsing PDS lookup response.");
        }
        return pdsAdaptorSuspensionStatusResponse;
    }

    private HttpEntity<String> prepareHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(SUSPENSION_SERVICE_USERNAME, suspensionServicePassword);

        HttpEntity<String> entity = new HttpEntity<String>(headers);

        return entity;
    }
}