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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


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
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponse())));
        stubFor(put(urlMatching("/suspended-patient-status/9912003888"))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getSuspendedResponse())
                        .withHeader("Content-Type", "application/json")));


        String queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        String mofUpdatedQueueUrl = sqs.getQueueUrl(mofUpdatedQueueName).getQueueUrl();

        sqs.sendMessageBatch(createBatchRequest(queueUrl));
        sqs.sendMessageBatch(createBatchRequest(queueUrl));
        sqs.sendMessageBatch(createBatchRequest(queueUrl));
        sqs.sendMessageBatch(createBatchRequest(queueUrl));
        sqs.sendMessageBatch(createBatchRequest(queueUrl));
        sqs.sendMessageBatch(createBatchRequest(queueUrl));
        sqs.sendMessageBatch(createBatchRequest(queueUrl));
        sqs.sendMessageBatch(createBatchRequest(queueUrl));

        checkMessageInRelatedQueue(queueUrl);

        sqs.purgeQueue(new PurgeQueueRequest(queueUrl));
        sqs.purgeQueue(new PurgeQueueRequest(mofUpdatedQueueUrl));

    }

    private void checkMessageInRelatedQueue(String queueUrl) {
        System.out.println("checking sqs queue: " + queueUrl);


        System.out.println("starting time is " + Instant.now().toString());

        while (!isQueueEmpty(queueUrl)) {
            System.out.println("continue processing");

        }
        System.out.println("finish is " + Instant.now().toString());

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

    private SendMessageBatchRequest createBatchRequest(String queueUrl) {
        SendMessageBatchRequest sendMessageBatchRequest = new SendMessageBatchRequest();
        sendMessageBatchRequest.setEntries(generateSendMessageBatchRequestEntryList());
        sendMessageBatchRequest.setQueueUrl(queueUrl);

        return sendMessageBatchRequest;
    }

    private List<SendMessageBatchRequestEntry> generateSendMessageBatchRequestEntryList() {
        int index = 0;
        List<SendMessageBatchRequestEntry> requestEntries = new ArrayList<>();
        while (index < 9) {
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
