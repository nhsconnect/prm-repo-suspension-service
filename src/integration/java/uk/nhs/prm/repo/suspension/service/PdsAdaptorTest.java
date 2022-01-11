package uk.nhs.prm.repo.suspension.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.repo.suspension.service.http.HttpServiceClient;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.LocalStackAwsConfig;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest()
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LocalStackAwsConfig.class)
@EnableScheduling
public class PdsAdaptorTest {

    private WireMockServer wireMockServer;

    @Autowired
    private HttpServiceClient httpServiceClient;

    private static final String SUSPENSION_SERVICE_USERNAME = "suspension-service";

    @Value("${pdsAdaptor.serviceUrl}")
    private String apiHost;

    @Value("pdsAdaptor.suspensionService.password")
    private String password;

    private final String EXTENSION="/suspended-patient-status/9912003888";

    @BeforeEach
    public void setUp(){
        wireMockServer = initializeWebServer();
    }

    @AfterEach
    public void tearDown(){
        wireMockServer.stop();
    }

    private WireMockServer initializeWebServer() {
        WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();

        return wireMockServer;
    }

    @Test
    void shouldProcessNotSuspendedPatient(){
        stubFor(get(urlMatching("/suspended-patient-status/9912003888"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs("Started")
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOnBkc0FkYXB0b3Iuc3VzcGVuc2lvblNlcnZpY2UucGFzc3dvcmQ="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getNotSuspendedResponse())));

        String response = httpServiceClient.get(apiHost+EXTENSION, SUSPENSION_SERVICE_USERNAME, password );
        assertTrue(response.contains("isSuspended"));
        assertTrue(response.contains("false"));
        assertTrue(response.contains("currentOdsCode"));
        assertTrue(response.contains("N85027"));

    }

    @Test
    void shouldProcessSuspendedPatient(){
        stubFor(get(urlMatching("/suspended-patient-status/9912003888"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs("Started")
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOnBkc0FkYXB0b3Iuc3VzcGVuc2lvblNlcnZpY2UucGFzc3dvcmQ="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));

        String response = httpServiceClient.get(apiHost+EXTENSION, SUSPENSION_SERVICE_USERNAME, password );
        assertTrue(response.contains("isSuspended"));
        assertTrue(response.contains("true"));
        assertTrue(response.contains("currentOdsCode"));
        assertTrue(response.contains("null"));

    }

    private String getNotSuspendedResponse() {
        return "{\n" +
                "    \"isSuspended\": false,\n" +
                "    \"currentOdsCode\": \"N85027\"\n" +
                "}";
    }

    private String getSuspendedResponse() {
        return "{\n" +
                "    \"isSuspended\": true,\n" +
                "    \"currentOdsCode\": null,\n" +
                "    \"managingOrganisation\": \"B1234\",\n" +
                "    \"recordETag\": \"W/\\\"5\\\"\"\n" +
                "}";
    }
}
