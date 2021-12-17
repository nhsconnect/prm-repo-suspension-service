package uk.nhs.prm.repo.suspension.service.pds;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;

import java.util.Arrays;

@Component
@AllArgsConstructor
public class PdsLookupService {

    private RestTemplate pdsAdaptorClient;

    private static final String SUSPENDED_PATIENT = "suspended-patient-status/";

    @Value("${pdsAdaptor.suspensionService.password}")
    private String suspensionServicePassword;

    @Autowired
    public PdsLookupService(RestTemplate pdsAdaptorClient) {
        this.pdsAdaptorClient = pdsAdaptorClient;
    }

    public PdsAdaptorSuspensionStatusResponse isSuspended(String nhsNumber) {
        ResponseEntity<PdsAdaptorSuspensionStatusResponse> responseEntity = pdsAdaptorClient
                .exchange(SUSPENDED_PATIENT + nhsNumber, HttpMethod.GET, prepareHeader(), PdsAdaptorSuspensionStatusResponse.class);
        return responseEntity.getBody();
    }

    private HttpEntity<String> prepareHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("suspension-service", suspensionServicePassword);

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        return entity;
    }
}