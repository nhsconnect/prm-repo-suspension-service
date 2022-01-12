package uk.nhs.prm.repo.suspension.service.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.repo.suspension.service.config.Tracer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HttpServiceClient.class, Tracer.class, RestTemplate.class})
public class HttpServiceClientTest {

    public static final String BOB_BANANA_AUTH_TOKEN = "Ym9iOmJhbmFuYQ==";
    private WireMockServer wireMockServer;

    @Autowired
    private HttpServiceClient httpServiceClient;

    @BeforeEach
    public void setUp() {
        wireMockServer = initializeWebServer();
    }

    @Test
    void shouldPerformGetWithAuthAndReturnBody() {
        stubFor(get(urlMatching("/get-path"))
                .withHeader("Authorization", matching("Basic " + BOB_BANANA_AUTH_TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("response body")));

        String responsePayload = httpServiceClient.get(
                "http://localhost:8080/get-path",
                "bob",
                "banana");

        assertThat(responsePayload).isEqualTo("response body");
    }

    @Test
    void shouldPerformPutWithAuthAndReturnBody() {
        stubFor(put(urlMatching("/put-path"))
                .withHeader("Authorization", matching("Basic " + BOB_BANANA_AUTH_TOKEN))
                .withRequestBody(equalTo("some request payload"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("some body")));

        String responsePayload = httpServiceClient.put(
                "http://localhost:8080/put-path",
                "bob",
                "banana",
                "some request payload");

        assertThat(responsePayload).isEqualTo("some body");
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    private WireMockServer initializeWebServer() {
        WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();

        return wireMockServer;
    }
}
