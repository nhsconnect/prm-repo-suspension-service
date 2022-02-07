package uk.nhs.prm.repo.suspension.service.pds;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.nhs.prm.repo.suspension.service.http.HttpServiceClient;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.model.UpdateManagingOrganisationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class PdsServiceTest {

    @Mock
    private HttpServiceClient client;

    @Mock
    private PdsAdaptorSuspensionStatusResponseParser responseParser;

    private PdsService pdsService;

    @BeforeEach
    public void setUp() {
        pdsService = new PdsService(responseParser, client);
        setField(pdsService, "serviceUrl", "http://pds-adaptor"); // @todo fertling :/
        setField(pdsService, "suspensionServicePassword", "PASS");
    }

    @Test
    public void shouldGetPatientStatusFromPdsServiceUrlAndParseResponse() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";
        var parsedStatus = aStatus();

        ResponseEntity<String> response = ResponseEntity.ok("some status");
        when(client.getWithStatusCode(expectedUrl, "suspension-service", "PASS"))
                .thenReturn(response);
        when(responseParser.parse("some status")).thenReturn(parsedStatus);

        var status = pdsService.isSuspended("1234567890");
        assertThat(status).isEqualTo(parsedStatus);
    }

    @Test
    public void shouldUpdatePatientStatusFromPdsServiceUrlAndParseResponse() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";
        var parsedStatus = aStatus();
        var requestPayload = new UpdateManagingOrganisationRequest("hello", "bob");

        ResponseEntity<String> response = ResponseEntity.ok("some status");
        when(client.putWithStatusCode(expectedUrl, "suspension-service", "PASS", requestPayload)).thenReturn(response);
        when(responseParser.parse("some status")).thenReturn(parsedStatus);

        var status = pdsService.updateMof("1234567890", "hello", "bob");
        assertThat(status).isEqualTo(parsedStatus);
    }

    @Test
    public void shouldThrowExceptionWhenPdsReturn404() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";

        ResponseEntity<String> response = ResponseEntity.notFound().build();
        when(client.getWithStatusCode(expectedUrl, "suspension-service", "PASS")).thenReturn(response);

        Assertions.assertThrows(InvalidPdsRequestException.class, () -> {
            pdsService.isSuspended("1234567890");
        });
    }

    @Test
    public void shouldThrowExceptionWhenPdsReturn400() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";

        when(client.getWithStatusCode(expectedUrl, "suspension-service", "PASS"))
                .thenThrow(HttpClientErrorException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () -> {
            pdsService.isSuspended("1234567890");
        });
    }

    @Test
    public void shouldThrowExceptionWhenPdsReturn400ForMofUpdate() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";
        var requestPayload = new UpdateManagingOrganisationRequest("hello", "bob");

        when(client.putWithStatusCode(expectedUrl, "suspension-service", "PASS", requestPayload ))
                .thenThrow(HttpClientErrorException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () -> {
            pdsService.updateMof("1234567890", "hello", "bob");
        });
    }

    @Test
    public void shouldThrowExceptionWhenPdsReturn500() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";

        when(client.getWithStatusCode(expectedUrl, "suspension-service", "PASS"))
                .thenThrow(HttpServerErrorException.class);

        Assertions.assertThrows(IntermittentErrorPdsException.class, () -> {
            pdsService.isSuspended("1234567890");
        });
    }

    @Test
    public void shouldThrowExceptionWhenPdsReturn500ForMofUpdate() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";
        var requestPayload = new UpdateManagingOrganisationRequest("hello", "bob");

        when(client.putWithStatusCode(expectedUrl, "suspension-service", "PASS", requestPayload ))
                .thenThrow(HttpServerErrorException.class);

        Assertions.assertThrows(IntermittentErrorPdsException.class, () -> {
            pdsService.updateMof("1234567890", "hello", "bob");
        });
    }

    @Test
    public void shouldThrowExceptionWhenPdsConnectionFails() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";

        when(client.getWithStatusCode(expectedUrl, "suspension-service", "PASS"))
                .thenThrow(RuntimeException.class);

        Assertions.assertThrows(IntermittentErrorPdsException.class, () -> {
            pdsService.isSuspended("1234567890");
        });
    }

    @Test
    public void shouldThrowExceptionWhenPdsConnectionFailsForMofUpdate() {
        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";
        var requestPayload = new UpdateManagingOrganisationRequest("hello", "bob");

        when(client.putWithStatusCode(expectedUrl, "suspension-service", "PASS", requestPayload ))
                .thenThrow(RuntimeException.class);

        Assertions.assertThrows(IntermittentErrorPdsException.class, () -> {
            pdsService.updateMof("1234567890", "hello", "bob");
        });
    }

    private PdsAdaptorSuspensionStatusResponse aStatus() {
        return new PdsAdaptorSuspensionStatusResponse("9692294951", true, "ODS123", "ODS456", "v1");
    }
}