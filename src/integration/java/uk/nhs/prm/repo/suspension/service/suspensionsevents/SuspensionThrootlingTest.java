package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest()
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LocalStackAwsConfig.class)
@EnableScheduling
public class SuspensionThrootlingTest {

    @Autowired
    private AmazonSQSAsync sqs;

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${aws.mofUpdatedQueueName}")
    private String mofUpdatedQueueName;

    private WireMockServer stubPdsAdaptor;

    private String mofUpdatedQueueUrl;
    private String suspensionQueueUrl;

    @BeforeEach
    public void setUp() {
        stubPdsAdaptor = initializeWebServer();
        mofUpdatedQueueUrl = sqs.getQueueUrl(mofUpdatedQueueName).getQueueUrl();
        suspensionQueueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        purgeQueues(suspensionQueueUrl, mofUpdatedQueueUrl);
    }

    @AfterEach
    public void tearDown() {
        stubPdsAdaptor.stop();
        purgeQueues(suspensionQueueUrl, mofUpdatedQueueUrl);
    }

    private final String suspensionEventForBackOff = new SuspensionEventBuilder()
            .lastUpdated("2017-11-01T15:00:33+00:00")
            .previousOdsCode("B85612")
            .eventType("SUSPENSION")
            .nhsNumber("1234567890")
            .nemsMessageId("TEST-NEMS-ID-BACK-OFF")
            .environment("local").buildJson();

    private WireMockServer initializeWebServer() {
        final WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
        return wireMockServer;
    }

    @Test
    void shouldProcess120MessagesIn60Seconds() {
        stubbinForGenericPdsResponses(200, 800);

        var startingTime = Instant.now();

        sendMultipleBatchesOf10Messages(suspensionQueueUrl, 12);

        await().atMost(120, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(isQueueEmpty(suspensionQueueUrl)));

        var finishTime = Instant.now();
        var timeElapsed = Duration.between(startingTime, finishTime);

        System.out.println("Total time taken: " + timeElapsed);

        assertThat(timeElapsed).isCloseTo(Duration.ofSeconds(65), Duration.ofSeconds(10));
    }

    @Test
    void shouldThrottlePdsAdaptorToPreventUpdatesMoreThan2PerSecond() {
        stubbinForGenericPdsResponses(0, 0);

        var startingTime = Instant.now();

        sendMultipleBatchesOf10Messages(suspensionQueueUrl, 5);

        await().atMost(120, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(isQueueEmpty(suspensionQueueUrl)));

        var finishTime = Instant.now();
        var timeElapsed = Duration.between(startingTime, finishTime);

        System.out.println("Total time taken: " + timeElapsed);

        assertThat(timeElapsed).isCloseTo(Duration.ofSeconds(20), Duration.ofSeconds(5));
    }

    @Test
    void shouldContinueProcessMessagesWhenOneFailsWithConcurrentThreads() {
        setPdsRetryMessage();
        stubbinForGenericPdsResponses(0,0);

        var startingTime = Instant.now();

        sqs.sendMessage(suspensionQueueUrl, suspensionEventForBackOff);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(isQueueEmpty(suspensionQueueUrl)));

        var finishTime = Instant.now();
        var timeElapsed = Duration.between(startingTime, finishTime);

        System.out.println("Total time taken: " + timeElapsed);

        assertThat(timeElapsed).isCloseTo(Duration.ofSeconds(8), Duration.ofSeconds(1));
        verify(4, getRequestedFor(urlEqualTo("/suspended-patient-status/1234567890")));
    }

    @Test
    void shouldRetry3TimesWithExponentialBackOffPeriod() {
        stubbinForGenericPdsResponses(200, 800);
        setPdsRetryMessage();

        var startingTime = Instant.now();

        sendMultipleBatchesOf10Messages(suspensionQueueUrl, 5);
        sqs.sendMessage(suspensionQueueUrl, suspensionEventForBackOff);

        await().atMost(120, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(isQueueEmpty(suspensionQueueUrl)));

        var finishTime = Instant.now();
        var timeElapsed = Duration.between(startingTime, finishTime);

        System.out.println("Total time taken: " + timeElapsed);

        assertThat(timeElapsed).isCloseTo(Duration.ofSeconds(35), Duration.ofSeconds(5));
    }


    private void setPdsRetryMessage() {
        setPdsErrorState(STARTED, "Cause Success", 1);
        setPdsErrorState("Cause Success", "Second Cause Success", 2);
        setPdsErrorState("Second Cause Success", "Third Cause Success", 3);

        stubFor(get(urlEqualTo("/suspended-patient-status/1234567890")).atPriority(4)
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Third Cause Success")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));
    }

    private void setPdsErrorState(String startingState, String finishedState, int priority) {
        stubFor(get(urlMatching("/suspended-patient-status/1234567890")).atPriority(priority)
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(startingState)
                .willReturn(aResponse()
                        .withStatus(500) // request unsuccessful with status code 500
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo(finishedState));
    }

    private void sendMultipleBatchesOf10Messages(String queueUrl, int numberOfBatches) {
        for (int i = 1; i <= numberOfBatches; i++) {
            sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        }
    }

    private void stubbinForGenericPdsResponses(int getRequestDelay, int putRequestDelay) {
        var anyNhsNumber = ".*";
        stubFor(get(urlMatching("/suspended-patient-status/" + anyNhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(getRequestDelay)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));
        stubFor(put(urlMatching("/suspended-patient-status/" + anyNhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(putRequestDelay)
                        .withBody(getSuspendedResponse())
                        .withHeader("Content-Type", "application/json")));
    }


    private boolean isQueueEmpty(String queueUrl) {
        List<String> attributeList = new ArrayList<>();
        attributeList.add("ApproximateNumberOfMessagesNotVisible");
        attributeList.add("ApproximateNumberOfMessages");
        GetQueueAttributesResult getQueueAttributesResult = sqs.getQueueAttributes(queueUrl, attributeList);

        Integer numberOfMessageNotVisible = Integer.valueOf(getQueueAttributesResult.getAttributes().get("ApproximateNumberOfMessagesNotVisible"));
        Integer numberOfMessageVisible = Integer.valueOf(getQueueAttributesResult.getAttributes().get("ApproximateNumberOfMessages"));

        if (numberOfMessageVisible == 0 && numberOfMessageNotVisible == 0) {
            return true;
        }
        return false;
    }

    private SendMessageBatchRequest createBatchOfTenRequest(String queueUrl) {
        SendMessageBatchRequest sendMessageBatchRequest = new SendMessageBatchRequest();
        sendMessageBatchRequest.setEntries(generateSendMessageBatchRequestEntryList());
        sendMessageBatchRequest.setQueueUrl(queueUrl);

        return sendMessageBatchRequest;
    }

    private List<SendMessageBatchRequestEntry> generateSendMessageBatchRequestEntryList() {
        List<SendMessageBatchRequestEntry> requestEntries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            requestEntries.add(new SendMessageBatchRequestEntry(UUID.randomUUID().toString(), getSuspensionEvent()));
        }
        return requestEntries;
    }

    private String getSuspensionEvent() {
        String nhsNumber = UUID.randomUUID().toString();
        System.out.println("Nhs Number generated: " + nhsNumber);
        return new SuspensionEventBuilder()
                .lastUpdated("2017-11-01T15:00:33+00:00")
                .previousOdsCode("B85612")
                .eventType("SUSPENSION")
                .nhsNumber(nhsNumber)
                .nemsMessageId("TEST-NEMS-ID")
                .environment("local").buildJson();
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

    private void purgeQueues(String queueUrl, String mofUpdatedQueueUrl) {
        sqs.purgeQueue(new PurgeQueueRequest(queueUrl));
        sqs.purgeQueue(new PurgeQueueRequest(mofUpdatedQueueUrl));
    }
}
