package uk.nhs.prm.repo.suspension.service.pds;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.repo.suspension.service.http.HttpServiceClient;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;

@Component
@AllArgsConstructor
@Slf4j
public class PdsLookupService {

    public static final String SUSPENSION_SERVICE_USERNAME = "suspension-service";
    private static final String SUSPENDED_PATIENT = "suspended-patient-status/";

    @Value("${pdsAdaptor.suspensionService.password}")
    private String suspensionServicePassword;

    private PdsAdaptorSuspensionStatusResponseParser responseParser;

    private HttpServiceClient httpClient;

    public PdsAdaptorSuspensionStatusResponse isSuspended(String nhsNumber) {
        final String url = SUSPENDED_PATIENT + nhsNumber;
        String responseBody = httpClient.get(url, SUSPENSION_SERVICE_USERNAME, suspensionServicePassword);
        return responseParser.parse(responseBody);
    }

}