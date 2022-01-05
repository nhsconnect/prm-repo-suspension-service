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

@ExtendWith(MockitoExtension.class)@ExtendWith(MockitoExtension.class)
public class PdsUpdateServiceTest {

    @Mock
    private RestTemplate client;

    @Mock
    private Tracer tracer;

    private PdsUpdateService pdsUpdateService;

    @BeforeEach
    public void setUp() {
        pdsUpdateService = new PdsUpdateService(new PdsAdaptorSuspensionStatusResponseParser(), new HttpServiceClient(client, tracer));
    }

    @Test
    void shouldUpdateMofWhenPatientSuspended(){
        ReflectionTestUtils.setField(pdsUpdateService, "suspensionServicePassword", "PASS");
        String myobjectA = "{\n" +
                "    \"isSuspended\": true,\n" +
                "    \"currentOdsCode\": null,\n" +
                "    \"managingOrganisation\": \"M85019\",\n" +
                "    \"recordETag\": \"W/\\\"11\\\"\"\n" +
                "}";
        ResponseEntity<String> myEntity =
                new ResponseEntity<>(myobjectA,HttpStatus.ACCEPTED);

        Mockito.when(client.exchange(
                ArgumentMatchers.eq("suspended-patient-status/123456789"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<String>>any())
        ).thenReturn(myEntity);

        PdsAdaptorSuspensionStatusResponse res = pdsUpdateService.updateMof("123456789", "PRVODS", "W/\"11\"");
        assertThat(res.getIsSuspended());
        assertThat(res.getCurrentOdsCode()).isNull();
        assertThat(res.getManagingOrganisation()).isEqualTo("M85019");
    }

}
