package uk.nhs.prm.repo.suspension.service.pds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.nhs.prm.repo.suspension.service.http.HttpServiceClient;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.model.UpdateManagingOrganisationRequest;

@Component
@Slf4j
public class PdsService {

    public static final String SUSPENSION_SERVICE_USERNAME = "suspension-service";
    private static final String SUSPENDED_PATIENT = "suspended-patient-status/";

    @Value("${pdsAdaptor.suspensionService.password}")
    private String suspensionServicePassword;

    @Value("${pdsAdaptor.serviceUrl}")
    private String serviceUrl;

    private final PdsAdaptorSuspensionStatusResponseParser responseParser;

    private final HttpServiceClient httpClient;

    public PdsService(PdsAdaptorSuspensionStatusResponseParser responseParser, HttpServiceClient httpClient) {
        this.responseParser = responseParser;
        this.httpClient = httpClient;
    }

    public PdsAdaptorSuspensionStatusResponse isSuspended(String nhsNumber) {
        final String url = getPatientUrl(nhsNumber);
        ResponseEntity<String> response = httpClient.getWithStatusCode(url, SUSPENSION_SERVICE_USERNAME, suspensionServicePassword);
        return responseParser.parse(response.getBody());
    }

    public PdsAdaptorSuspensionStatusResponse updateMof(String nhsNumber, String previousOdsCode, String recordETag) {
        log.info("Making request to update Managing Organization field");
        final String url = getPatientUrl(nhsNumber);
        final UpdateManagingOrganisationRequest requestPayload = new UpdateManagingOrganisationRequest(previousOdsCode, recordETag);

        ResponseEntity<String> response = httpClient.putWithStatusCode(url, SUSPENSION_SERVICE_USERNAME, suspensionServicePassword, requestPayload);
        return responseParser.parse(response.getBody());
    }

    private String getPatientUrl(String nhsNumber) {
        return serviceUrl + "/" + SUSPENDED_PATIENT + nhsNumber;
    }
}
