package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

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
            .nhsNumber(getStubNhsNumber())
            .nemsMessageId("TEST-NEMS-ID")
            .environment("local").buildJson();

    private String suspensionEventForBackOff = new SuspensionEventBuilder()
            .lastUpdated("2017-11-01T15:00:33+00:00")
            .previousOdsCode("B85612")
            .eventType("SUSPENSION")
            .nhsNumber("1234567890")
            .nemsMessageId("TEST-NEMS-ID-BACK-OFF")
            .environment("local").buildJson();

    private String getStubNhsNumber() {
        return UUID.randomUUID().toString();
    }

    private WireMockServer initializeWebServer() {
        final WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
        return wireMockServer;
    }

    @Test
    void shouldProcess120MessagesIn60Seconds() {
        stubbinForGenericPdsResponses();

        var queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        var mofUpdatedQueueUrl = sqs.getQueueUrl(mofUpdatedQueueName).getQueueUrl();
        var startingTime = Instant.now();

        send120MessageToSuspensionQueue(queueUrl);

        checkMessageInRelatedQueue(queueUrl);

        var finishTime = Instant.now();
        var timeElapsed = Duration.between(startingTime, finishTime);

        sqs.purgeQueue(new PurgeQueueRequest(queueUrl));
        sqs.purgeQueue(new PurgeQueueRequest(mofUpdatedQueueUrl));

        var lowerBound = Duration.ofSeconds(57);
        var upperBound = Duration.ofSeconds(63);

        assertThat(timeElapsed).isBetween(lowerBound, upperBound);
    }

    @Test
    void shouldTryToProcessMultipleTimesWhenPdsReturn500() {
        stubbinForGenericPdsResponses();

        stubFor(get(urlMatching("/suspended-patient-status/1234567890")).atPriority(1)
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(500) // request unsuccessful with status code 500
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Cause Success"));

        // Second StubMapping
        stubFor(get(urlEqualTo("/suspended-patient-status/1234567890")).atPriority(2)
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Cause Success")
                .willReturn(aResponse()
                        .withStatus(500)  // request successful with status code 200
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Second Cause Success"));

        stubFor(get(urlEqualTo("/suspended-patient-status/1234567890")).atPriority(3)
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Cause Success")
                .willReturn(aResponse()
                        .withStatus(500)  // request successful with status code 200
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Third Cause Success"));

        // Third StubMapping
        stubFor(get(urlEqualTo("/suspended-patient-status/1234567890")).atPriority(4)
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Third Cause Success")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));



        var queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        var mofUpdatedQueueUrl = sqs.getQueueUrl(mofUpdatedQueueName).getQueueUrl();
        var startingTime = Instant.now();

        send120MessageToSuspensionQueue(queueUrl);

        sqs.sendMessage(queueUrl,suspensionEventForBackOff);

        checkMessageInRelatedQueue(queueUrl);

        var finishTime = Instant.now();
        var timeElapsed = Duration.between(startingTime, finishTime);

        sqs.purgeQueue(new PurgeQueueRequest(queueUrl));
        sqs.purgeQueue(new PurgeQueueRequest(mofUpdatedQueueUrl));

        var normalProcessionTimePlusBackOffDelayLowerBond = Duration.ofSeconds(64);
        var normalProcessionTimePlusBackOffDelayUpperBound = Duration.ofSeconds(70);

        assertThat(timeElapsed).isBetween(normalProcessionTimePlusBackOffDelayLowerBond, normalProcessionTimePlusBackOffDelayUpperBound);
    }

    private void send120MessageToSuspensionQueue(String queueUrl) {
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
        sqs.sendMessageBatch(createBatchOfTenRequest(queueUrl));
    }

    private void stubbinForGenericPdsResponses() {
        var anyNhsNumber = ".*";
        stubFor(get(urlMatching("/suspended-patient-status/" + anyNhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));
        stubFor(put(urlMatching("/suspended-patient-status/" + anyNhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getSuspendedResponse())
                        .withHeader("Content-Type", "application/json")));
    }

    private void checkMessageInRelatedQueue(String queueUrl) {
        System.out.println("checking sqs queue: " + queueUrl);
        while (!isQueueEmpty(queueUrl)) {
            System.out.println("continue processing");
        }
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
        int index = 0;
        List<SendMessageBatchRequestEntry> requestEntries = new ArrayList<>();
        while (index < 10) {
            requestEntries.add(new SendMessageBatchRequestEntry(UUID.randomUUID().toString(), suspensionEvent));
            index++;
        }
        return requestEntries;
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
}
