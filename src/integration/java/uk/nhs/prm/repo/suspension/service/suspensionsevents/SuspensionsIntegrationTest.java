package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest()
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LocalStackAwsConfig.class)
@EnableScheduling
public class SuspensionsIntegrationTest {

    @Autowired
    private AmazonSQSAsync sqs;

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${aws.notSuspendedQueueName}")
    private String notSuspendedQueueName;

    @Value("${aws.mofUpdatedQueueName}")
    private String mofUpdatedQueueName;

    @Value("${aws.nonSensitiveInvalidSuspensionQueueName}")
    private String nonSensitiveInvalidSuspensionQueueName;

    @Value("${aws.invalidSuspensionQueueName}")
    private String invalidSuspensionQueueName;

    private WireMockServer stubPdsAdaptor;

    @BeforeEach
    public void setUp() {
        stubPdsAdaptor = initializeWebServer();
    }

    @AfterEach
    public void tearDown() {
        stubPdsAdaptor.stop();
    }

    private String suspensionEvent = new SuspensionEventBuilder()
            .lastUpdated("2017-11-01T15:00:33+00:00")
            .previousOdsCode("B85612")
            .eventType("SUSPENSION")
            .nhsNumber("9912003888")
            .nemsMessageId("TEST-NEMS-ID")
            .environment("local").buildJson();
    
     private WireMockServer initializeWebServer() {
        final WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();

        return wireMockServer;
    }

    @Test
    void shouldSendSuspensionMessageToNotSuspendedSNSTopicIfNoLongerSuspendedInPDS() {
        stubFor(get(urlMatching("/suspended-patient-status/9912003888"))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getNotSuspendedResponse())));

        String queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        String notSuspendedQueueUrl = sqs.getQueueUrl(notSuspendedQueueName).getQueueUrl();
        sqs.sendMessage(queueUrl, suspensionEvent);

        Message[] receivedMessageHolder = new Message[1];
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            checkMessageInRelatedQueue(notSuspendedQueueUrl, receivedMessageHolder);

            assertTrue(receivedMessageHolder[0].getBody().contains("lastUpdated"));
            assertTrue(receivedMessageHolder[0].getBody().contains("B85612"));
            assertTrue(receivedMessageHolder[0].getMessageAttributes().containsKey("traceId"));
        });
        purgeQueue(notSuspendedQueueUrl);
    }

    @Test
    void shouldUpdateManagingOrganisationAndSendMessageToMofUpdatedSNSTopicForSuspendedPatient() {
        stubFor(get(urlMatching("/suspended-patient-status/9912003888"))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));
        stubFor(put(urlMatching("/suspended-patient-status/9912003888"))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));

        String queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        String mofUpdatedQueueUrl = sqs.getQueueUrl(mofUpdatedQueueName).getQueueUrl();
        sqs.sendMessage(queueUrl, suspensionEvent);

        Message[] receivedMessageHolder = new Message[1];
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            checkMessageInRelatedQueue(mofUpdatedQueueUrl, receivedMessageHolder);

            assertTrue(receivedMessageHolder[0].getBody().contains("nhsNumber"));
            assertTrue(receivedMessageHolder[0].getBody().contains("9912003888"));
            assertTrue(receivedMessageHolder[0].getBody().contains("managingOrganisationOdsCode"));
            assertTrue(receivedMessageHolder[0].getBody().contains("B1234"));
            assertTrue(receivedMessageHolder[0].getMessageAttributes().containsKey("traceId"));
        });
        purgeQueue(mofUpdatedQueueUrl);
    }

    @Test
    void shouldPutDLQsWhenPdsAdaptorReturn400(){
        stubFor(get(urlMatching("/suspended-patient-status/9912003888"))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));
        stubFor(put(urlMatching("/suspended-patient-status/9912003888"))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")));

        String queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        String invalidSuspensionQueueUrl = sqs.getQueueUrl(invalidSuspensionQueueName).getQueueUrl();
        String nonSensitiveInvalidSuspensionQueueUrl = sqs.getQueueUrl(nonSensitiveInvalidSuspensionQueueName).getQueueUrl();
        sqs.sendMessage(queueUrl, suspensionEvent);

        Message[] receivedMessageHolderForInvalidSuspensions = new Message[1];
        Message[] receivedMessageHolderForNonSensitiveInvalidSuspensions = new Message[1];

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            checkMessageInRelatedQueue(invalidSuspensionQueueUrl, receivedMessageHolderForInvalidSuspensions);
            checkMessageInRelatedQueue(nonSensitiveInvalidSuspensionQueueUrl, receivedMessageHolderForNonSensitiveInvalidSuspensions);

            assertTrue(receivedMessageHolderForNonSensitiveInvalidSuspensions[0].getBody().contains("NO_ACTION:INVALID_SUSPENSION"));
            assertTrue(receivedMessageHolderForNonSensitiveInvalidSuspensions[0].getBody().contains("TEST-NEMS-ID"));

            assertTrue(receivedMessageHolderForInvalidSuspensions[0].getBody().contains("nhsNumber"));
            assertTrue(receivedMessageHolderForInvalidSuspensions[0].getBody().contains("9912003888"));
            assertTrue(receivedMessageHolderForInvalidSuspensions[0].getBody().contains("managingOrganisationOdsCode"));
            assertTrue(receivedMessageHolderForInvalidSuspensions[0].getBody().contains("B1234"));
            assertTrue(receivedMessageHolderForInvalidSuspensions[0].getMessageAttributes().containsKey("traceId"));

        });
        purgeQueue(invalidSuspensionQueueUrl);
        purgeQueue(nonSensitiveInvalidSuspensionQueueUrl);

    }

    private void checkMessageInRelatedQueue(String queueUrl, Message[] receivedMessageHolder) {
        System.out.println("checking sqs queue: " + queueUrl);

        ReceiveMessageRequest requestForMessagesWithAttributes
                = new ReceiveMessageRequest().withQueueUrl(queueUrl)
                .withMessageAttributeNames("traceId");
        List<Message> messages = sqs.receiveMessage(requestForMessagesWithAttributes).getMessages();
        assertThat(messages).hasSize(1);
        receivedMessageHolder[0] = messages.get(0);
    }

    private String getNotSuspendedResponse() {
        return "{\n" +
            "    \"nhsNumber\": \"9912003888\",\n" +
            "    \"isSuspended\": false,\n" +
            "    \"currentOdsCode\": \"N85027\"\n" +
            "}";
    }

    private String getSuspendedResponse() {
        return "{\n" +
                "    \"nhsNumber\": \"9912003888\",\n" +
                "    \"isSuspended\": true,\n" +
                "    \"currentOdsCode\": null,\n" +
                "    \"managingOrganisation\": \"B1234\",\n" +
                "    \"recordETag\": \"W/\\\"5\\\"\"\n" +
                "}";
    }

    private void purgeQueue(String queueUrl) {
        System.out.println("Purging queue url: " + queueUrl);
        sqs.purgeQueue(new PurgeQueueRequest(queueUrl));
    }
}
