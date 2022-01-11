package uk.nhs.prm.repo.suspension.service.pds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.repo.suspension.service.config.Tracer;
import uk.nhs.prm.repo.suspension.service.http.HttpServiceClient;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@ExtendWith(MockitoExtension.class)
class PdsLookupTest {

    @Mock
    private RestTemplate client;

    private PdsLookupService pdsLookupService;

    @Mock
    private Tracer tracer;

    @BeforeEach
    public void setUp() {
        pdsLookupService = new PdsLookupService(new PdsAdaptorSuspensionStatusResponseParser(), new HttpServiceClient(client, tracer));
    }

    @Test
    public void getPdsResponseAsNotSuspended() {
        ReflectionTestUtils.setField(pdsLookupService, "suspensionServicePassword", "PASS");
        String myobjectA = "{\n" +
                "    \"isSuspended\": false,\n" +
                "    \"currentOdsCode\": \"B86041\",\n" +
                "    \"managingOrganisation\": null,\n" +
                "    \"recordETag\": \"W/\\\"5\\\"\"\n" +
                "}";
        ResponseEntity<String> myEntity =
                new ResponseEntity<String>(myobjectA,HttpStatus.ACCEPTED);

        Mockito.when(tracer.getTraceId()).thenReturn("12345678");

        Mockito.when(client.exchange(
                ArgumentMatchers.eq("suspended-patient-status/123456789"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<?>>any(),
                ArgumentMatchers.<Class<String>>any())
        ).thenReturn(myEntity);

        PdsAdaptorSuspensionStatusResponse res = pdsLookupService.isSuspended("123456789");
        assertThat(!res.getIsSuspended());
        assertThat(res.getCurrentOdsCode()).isEqualTo("B86041");
    }

    @Test
    public void getPdsResponseAsSuspended() {
        ReflectionTestUtils.setField(pdsLookupService, "suspensionServicePassword", "PASS");
        String myobjectA = "{\n" +
                "    \"isSuspended\": true,\n" +
                "    \"currentOdsCode\": null,\n" +
                "    \"managingOrganisation\": \"M85019\",\n" +
                "    \"recordETag\": \"W/\\\"11\\\"\"\n" +
                "}";
        ResponseEntity<String> myEntity =
                new ResponseEntity<String>(myobjectA,HttpStatus.ACCEPTED);

        Mockito.when(client.exchange(
                ArgumentMatchers.eq("suspended-patient-status/123456789"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.<HttpEntity<?>>any(),
                ArgumentMatchers.<Class<String>>any())
        ).thenReturn(myEntity);

        PdsAdaptorSuspensionStatusResponse res = pdsLookupService.isSuspended("123456789");
        assertThat(res.getIsSuspended());
        assertThat(res.getCurrentOdsCode()).isNull();
        assertThat(res.getManagingOrganisation()).isEqualTo("M85019");
    }
}