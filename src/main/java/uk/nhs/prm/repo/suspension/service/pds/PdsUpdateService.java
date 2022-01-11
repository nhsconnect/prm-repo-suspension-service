package uk.nhs.prm.repo.suspension.service.pds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.repo.suspension.service.http.HttpServiceClient;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.model.UpdateManagingOrganisationRequest;

@Component
@Slf4j
public class PdsUpdateService {

    public static final String SUSPENSION_SERVICE_USERNAME = "suspension-service";
    private static final String SUSPENDED_PATIENT = "suspended-patient-status/";

    @Value("${pdsAdaptor.suspensionService.password}")
    private String suspensionServicePassword;

    private final PdsAdaptorSuspensionStatusResponseParser responseParser;

    private final HttpServiceClient httpClient;

    public PdsUpdateService(PdsAdaptorSuspensionStatusResponseParser responseParser, HttpServiceClient httpClient) {
        this.responseParser = responseParser;
        this.httpClient = httpClient;
    }

    public PdsAdaptorSuspensionStatusResponse updateMof(String nhsNumber, String previousOdsCode, String recordETag) {
        final String url = SUSPENDED_PATIENT + nhsNumber;
        final UpdateManagingOrganisationRequest requestPayload = new UpdateManagingOrganisationRequest(previousOdsCode, recordETag);
        String responseBody = httpClient.put(url, SUSPENSION_SERVICE_USERNAME, suspensionServicePassword, requestPayload);
        return responseParser.parse(responseBody);
    }
}
