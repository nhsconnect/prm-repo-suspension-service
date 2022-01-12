package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest()
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {LocalStackAwsConfig.class, RestTemplate.class})
@EnableScheduling
public class SuspensionsIntegrationTest {

    @Autowired
    private AmazonSQSAsync amazonSQSAsync;

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${aws.notSuspendedQueueName}")
    private String notSuspendedQueueName;

    @Value("${aws.mofUpdatedQueueName}")
    private String mofUpdatedQueueName;

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setUp(){
        wireMockServer = initializeWebServer();
    }

    @AfterEach
    public void tearDown(){
        wireMockServer.stop();
    }

    private String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9912003888\"}\",\"environment\":\"local\"}";

    private WireMockServer initializeWebServer() {
        WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();

        return wireMockServer;
    }

    @Test
    void shouldSendMessageToNotSuspendedSNSTopic(){

        stubFor(get(urlMatching("/suspended-patient-status/9912003888"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs("Started")
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getNotSuspendedResponse())));

        String queueUrl = amazonSQSAsync.getQueueUrl(suspensionsQueueName).getQueueUrl();
        String notSuspendedQueueUrl = amazonSQSAsync.getQueueUrl(notSuspendedQueueName).getQueueUrl();
        System.out.println("queue url is: " + queueUrl);
        amazonSQSAsync.sendMessage(queueUrl, sampleMessage);

        Message[] receivedMessageHolder = new Message[1];
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            System.out.println("checking sqs queue: " + notSuspendedQueueUrl);

            ReceiveMessageRequest requestForMessagesWithAttributes
                    = new ReceiveMessageRequest().withQueueUrl(notSuspendedQueueUrl)
                    .withMessageAttributeNames("traceId");
            List<Message> messages = amazonSQSAsync.receiveMessage(requestForMessagesWithAttributes).getMessages();
            System.out.println("messages: " + messages.size());
            assertThat(messages).hasSize(1);
            receivedMessageHolder[0] = messages.get(0);
            System.out.println("message: " + messages.get(0).getBody());
            System.out.println("message attributes: " + messages.get(0).getMessageAttributes());
            System.out.println("message attributes empty: " + messages.get(0).getMessageAttributes().isEmpty());

            assertTrue(receivedMessageHolder[0].getBody().contains("lastUpdated"));
            assertTrue(receivedMessageHolder[0].getBody().contains("B85612"));
            assertTrue(receivedMessageHolder[0].getMessageAttributes().containsKey("traceId"));
        });
    }

    @Test
    void shouldSendMessageToMofUpdatedSNSTopic(){

        stubFor(get(urlMatching("/suspended-patient-status/9912003888"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs("Started")
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));
        stubFor(put(urlMatching("/suspended-patient-status/9912003888"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs("Started")
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));

        String queueUrl = amazonSQSAsync.getQueueUrl(suspensionsQueueName).getQueueUrl();
        String mofUpdatedQueueUrl = amazonSQSAsync.getQueueUrl(mofUpdatedQueueName).getQueueUrl();
        System.out.println("queue url is: " + queueUrl);
        amazonSQSAsync.sendMessage(queueUrl, sampleMessage);

        Message[] receivedMessageHolder = new Message[1];
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            System.out.println("checking sqs queue: " + mofUpdatedQueueUrl);

            ReceiveMessageRequest requestForMessagesWithAttributes
                    = new ReceiveMessageRequest().withQueueUrl(mofUpdatedQueueUrl)
                    .withMessageAttributeNames("traceId");
            List<Message> messages = amazonSQSAsync.receiveMessage(requestForMessagesWithAttributes).getMessages();
            System.out.println("messages: " + messages.size());
            assertThat(messages).hasSize(1);
            receivedMessageHolder[0] = messages.get(0);
            System.out.println("message: " + messages.get(0).getBody());
            System.out.println("message attributes: " + messages.get(0).getMessageAttributes());
            System.out.println("message attributes empty: " + messages.get(0).getMessageAttributes().isEmpty());

            assertTrue(receivedMessageHolder[0].getBody().contains("nhsNumber"));
            assertTrue(receivedMessageHolder[0].getBody().contains("9912003888"));
            assertTrue(receivedMessageHolder[0].getBody().contains("managingOrgainsationOdsCode"));
            assertTrue(receivedMessageHolder[0].getBody().contains("B1234"));
            assertTrue(receivedMessageHolder[0].getMessageAttributes().containsKey("traceId"));
        });
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
