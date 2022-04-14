package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;



@SpringBootTest(properties = { "process_only_synthetic_patients=true" })
public class SyntheticPatientTest extends SuspensionIntegrationTestSetup {

    @Autowired
    private AmazonSQSAsync sqs;

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${aws.mofUpdatedQueueName}")
    private String mofUpdatedQueueName;

    @Value("${aws.mofNotUpdatedQueueName}")
    private String mofNotUpdatedQueueName;


    @Test
    void shouldUpdateManagingOrganisationAndSendMessageToMofUpdatedForSyntheticPatient() {
        var nhsNumber = "9910123456";
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
        verify(1, getRequestedFor(urlEqualTo("/suspended-patient-status/" + nhsNumber)));
        verify(1, putRequestedFor(urlEqualTo("/suspended-patient-status/" + nhsNumber)));
        purgeQueue(mofUpdatedQueueUrl);
    }

    @Test
    void shouldNotUpdateManagingOrganisationAndSendMessageToMofNotUpdatedForNonSyntheticPatient() {
        var nhsNumber = "1110123456";
        stubFor(get(urlMatching("/suspended-patient-status/" + nhsNumber))
                .withHeader("Authorization", matching("Basic c3VzcGVuc2lvbi1zZXJ2aWNlOiJ0ZXN0Ig=="))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getSuspendedResponseWith(nhsNumber))));

        var queueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        var mofNotUpdatedQueue = sqs.getQueueUrl(mofNotUpdatedQueueName).getQueueUrl();
        sqs.sendMessage(queueUrl, getSuspensionEventWith(nhsNumber));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> receivedMessageHolder = checkMessageInRelatedQueue(mofNotUpdatedQueue);

            assertTrue(receivedMessageHolder.get(0).getBody().contains("NO_ACTION:NOT_SYNTHETIC"));
            assertTrue(receivedMessageHolder.get(0).getBody().contains("TEST-NEMS-ID"));
            assertTrue(receivedMessageHolder.get(0).getMessageAttributes().containsKey("traceId"));
        });
        verify(1, getRequestedFor(urlEqualTo("/suspended-patient-status/" + nhsNumber)));
        verify(0, putRequestedFor(urlEqualTo("/suspended-patient-status/" + nhsNumber)));
        purgeQueue(mofNotUpdatedQueue);
    }
}
