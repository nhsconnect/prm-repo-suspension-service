package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SuspensionsIntegrationTest extends SuspensionIntegrationTestSetup {

    @Autowired
    private AmazonSQSAsync sqs;

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${aws.notSuspendedQueueName}")
    private String notSuspendedQueueName;

    @Value("${aws.mofUpdatedQueueName}")
    private String mofUpdatedQueueName;

    @Value("${aws.eventOutOfOrderAuditName}")
    private String eventOutOfOrderAuditName;

    @Value("${aws.eventOutOfOrderObservabilityQueueName}")
    private String eventOutOfOrderObservabilityQueueName;

    @Value("${aws.nonSensitiveInvalidSuspensionQueueName}")
    private String nonSensitiveInvalidSuspensionQueueName;

    @Value("${aws.invalidSuspensionQueueName}")
    private String invalidSuspensionQueueName;


    @Test
    void shouldSendSuspensionMessageToNotSuspendedSNSTopicIfNoLongerSuspendedInPDS() {
        var nhsNumber = Long.toString(System.currentTimeMillis());
        stubFor(get(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getNotSuspendedResponseWith(nhsNumber))));

        var queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        var notSuspendedQueueUrl = sqs.getQueueUrl(notSuspendedQueueName).getQueueUrl();
        sqs.sendMessage(queueUrl, getSuspensionEventWith(nhsNumber));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> receivedMessageHolder = checkMessageInRelatedQueue(notSuspendedQueueUrl);
            assertTrue(receivedMessageHolder.get(0).getBody().contains("NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS"));
            assertTrue(receivedMessageHolder.get(0).getBody().contains("nemsMessageId"));
            assertTrue(receivedMessageHolder.get(0).getMessageAttributes().containsKey("traceId"));
        });
        purgeQueue(notSuspendedQueueUrl);
    }

    @Test
    void shouldUpdateManagingOrganisationAndSendMessageToMofUpdatedSNSTopicForSuspendedPatient() {
        var nhsNumber = Long.toString(System.currentTimeMillis());
        stubFor(get(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponseWith(nhsNumber))));
        stubFor(put(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponseWith(nhsNumber))));

        var queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        var mofUpdatedQueueUrl = sqs.getQueueUrl(mofUpdatedQueueName).getQueueUrl();
        sqs.sendMessage(queueUrl, getSuspensionEventWith(nhsNumber));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> receivedMessageHolder = checkMessageInRelatedQueue(mofUpdatedQueueUrl);

            assertTrue(receivedMessageHolder.get(0).getBody().contains("ACTION:UPDATED_MANAGING_ORGANISATION"));
            assertTrue(receivedMessageHolder.get(0).getBody().contains("TEST-NEMS-ID"));
            assertTrue(receivedMessageHolder.get(0).getBody().contains("B85612"));
            assertTrue(receivedMessageHolder.get(0).getMessageAttributes().containsKey("traceId"));
        });
        purgeQueue(mofUpdatedQueueUrl);
    }

    @Test
    void shouldPutEventOutOfOrderInRelevantQueues() {
        var nhsNumber = Long.toString(System.currentTimeMillis());

        stubFor(get(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponseWith(nhsNumber))));
        stubFor(put(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponseWith(nhsNumber))));

        var queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        var mofUpdatedQueueUrl = sqs.getQueueUrl(mofUpdatedQueueName).getQueueUrl();
        var eventOutOfOrderAuditQueueUrl = sqs.getQueueUrl(eventOutOfOrderAuditName).getQueueUrl();
        var eventOutOfOrderObservabilityQueueUrl = sqs.getQueueUrl(eventOutOfOrderObservabilityQueueName).getQueueUrl();

        var suspensionEvent = getSuspensionEventWith(nhsNumber);
        sqs.sendMessage(queueUrl, suspensionEvent);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var receivedMessageHolder = checkMessageInRelatedQueue(mofUpdatedQueueUrl);
            assertTrue(receivedMessageHolder.get(0).getBody().contains("B85612"));
        });

        var nemsMessageId = "OUT-OF-ORDER-ID";
        var secondSuspensionEvent = getSuspensionEventWith(nhsNumber, nemsMessageId);

        sqs.sendMessage(queueUrl, secondSuspensionEvent);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var receivedMessageInAuditQueue = checkMessageInRelatedQueue(eventOutOfOrderAuditQueueUrl);
            var receivedMessageInObservabilityQueue = checkMessageInRelatedQueue(eventOutOfOrderObservabilityQueueUrl);
            assertTrue(receivedMessageInAuditQueue.get(0).getBody().contains(nemsMessageId));
            assertTrue(receivedMessageInObservabilityQueue.get(0).getBody().contains(nemsMessageId));
        });

        purgeQueue(mofUpdatedQueueUrl);
        purgeQueue(eventOutOfOrderAuditQueueUrl);
        purgeQueue(eventOutOfOrderObservabilityQueueUrl);
    }

    @Test
    void shouldPutDLQsWhenPdsAdaptorReturn400() {
        var nhsNumber = Long.toString(System.currentTimeMillis());
        stubFor(get(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponseWith(nhsNumber))));
        stubFor(put(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")));

        var queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        var invalidSuspensionQueueUrl = sqs.getQueueUrl(invalidSuspensionQueueName).getQueueUrl();
        var nonSensitiveInvalidSuspensionQueueUrl = sqs.getQueueUrl(nonSensitiveInvalidSuspensionQueueName).getQueueUrl();
        sqs.sendMessage(queueUrl, getSuspensionEventWith(nhsNumber));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> receivedMessageHolderForInvalidSuspensions = checkMessageInRelatedQueue(invalidSuspensionQueueUrl);
            List<Message> receivedMessageHolderForNonSensitiveInvalidSuspensions = checkMessageInRelatedQueue(nonSensitiveInvalidSuspensionQueueUrl);

            assertTrue(receivedMessageHolderForNonSensitiveInvalidSuspensions.get(0).getBody().contains("NO_ACTION:INVALID_SUSPENSION"));
            assertTrue(receivedMessageHolderForNonSensitiveInvalidSuspensions.get(0).getBody().contains("TEST-NEMS-ID"));

            assertTrue(receivedMessageHolderForInvalidSuspensions.get(0).getBody().contains("nhsNumber"));
            assertTrue(receivedMessageHolderForInvalidSuspensions.get(0).getBody().contains(nhsNumber));
            assertTrue(receivedMessageHolderForInvalidSuspensions.get(0).getBody().contains("B85612"));
            assertTrue(receivedMessageHolderForInvalidSuspensions.get(0).getBody().contains("TEST-NEMS-ID"));
            assertTrue(receivedMessageHolderForInvalidSuspensions.get(0).getMessageAttributes().containsKey("traceId"));

        });
        purgeQueue(invalidSuspensionQueueUrl);
        purgeQueue(nonSensitiveInvalidSuspensionQueueUrl);

    }

    private String getNotSuspendedResponseWith(String nhsNumber) {
        return "{\n" +
                "    \"nhsNumber\": \"" + nhsNumber + "\",\n" +
                "    \"isSuspended\": false,\n" +
                "    \"currentOdsCode\": \"N85027\",\n" +
                "    \"isDeceased\":  false\n" +
                "}";
    }

}
