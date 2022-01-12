package uk.nhs.prm.repo.suspension.service.pds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.repo.suspension.service.config.Tracer;
import uk.nhs.prm.repo.suspension.service.http.HttpServiceClient;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
    }

    @Test
    public void shouldGetPatientStatusFromPdsServiceUrlAndParseResponse() {
        setField(pdsService, "serviceUrl", "http://pds-adaptor"); // @todo fertling :/
        setField(pdsService, "suspensionServicePassword", "PASS");

        String expectedUrl = "http://pds-adaptor/suspended-patient-status/1234567890";
        var parsedStatus = aStatus();

        when(client.get(expectedUrl, "suspension-service","PASS")).thenReturn("some status");
        when(responseParser.parse("some status")).thenReturn(parsedStatus);

        var status = pdsService.isSuspended("1234567890");
        assertThat(status).isEqualTo(parsedStatus);
    }

    private PdsAdaptorSuspensionStatusResponse aStatus() {
        return new PdsAdaptorSuspensionStatusResponse(true, "ODS123", "ODS456", "v1");
    }
}