package uk.nhs.prm.repo.suspension.service.pds;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Component
public class PdsLookupService {
    @Autowired
    private RestTemplate pdsAdaptorClient;

    private final String SUSPENDED_PATIENT = "suspended-patient-status/{nhsNumber}";

    @Value("${pdsAdaptor.suspensionService.password}")
    private String suspensionServicePassword;

    public boolean isSuspended(String nhsNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("suspension-service", suspensionServicePassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = pdsAdaptorClient
                .exchange("/suspended-patient-status/" + nhsNumber, HttpMethod.GET, entity, String.class);
        System.out.println(responseEntity.toString());
        return true;
    }
}