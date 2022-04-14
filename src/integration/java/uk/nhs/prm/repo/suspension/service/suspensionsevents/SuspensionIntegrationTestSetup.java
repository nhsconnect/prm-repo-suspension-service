package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.repo.suspension.service.infra.LocalStackAwsConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LocalStackAwsConfig.class)
@EnableScheduling
public class SuspensionIntegrationTestSetup {

    @Autowired
    private AmazonSQSAsync sqs;

    private WireMockServer stubPdsAdaptor;

    private String suspensionQueueUrl;

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @BeforeEach
    public void setUp() {
        stubPdsAdaptor = initializeWebServer();
        suspensionQueueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        purgeQueue(suspensionQueueUrl);
    }

    @AfterEach
    public void tearDown() {
        stubPdsAdaptor.stop();
        suspensionQueueUrl = sqs.getQueueUrl(suspensionsQueueName).getQueueUrl();
        purgeQueue(suspensionQueueUrl);
    }

    protected WireMockServer initializeWebServer() {
        final WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
        return wireMockServer;
    }

    protected void purgeQueue(String queueUrl) {
        System.out.println("Purging queue url: " + queueUrl);
        sqs.purgeQueue(new PurgeQueueRequest(queueUrl));
    }

    protected String getSuspendedResponseWith(String nhsNumber) {
        return "{\n" +
                "    \"nhsNumber\": \"" + nhsNumber + "\",\n" +
                "    \"isSuspended\": true,\n" +
                "    \"currentOdsCode\": null,\n" +
                "    \"managingOrganisation\": \"B1234\",\n" +
                "    \"recordETag\": \"W/\\\"5\\\"\",\n" +
                "    \"isDeceased\":  false\n" +
                "}";
    }

    protected String getSuspensionEventWith(String nhsNumber) {
        return getSuspensionEventWith(nhsNumber, "TEST-NEMS-ID");
    }

    protected String getSuspensionEventWith(String nhsNumber, String nemsMessageId) {
        return new SuspensionEventBuilder()
                .lastUpdated("2017-11-01T15:00:33+00:00")
                .previousOdsCode("B85612")
                .eventType("SUSPENSION")
                .nhsNumber(nhsNumber)
                .nemsMessageId(nemsMessageId)
                .environment("local").buildJson();
    }


    protected List<Message> checkMessageInRelatedQueue(String queueUrl) {
        System.out.println("checking sqs queue: " + queueUrl);

        var requestForMessagesWithAttributes
                = new ReceiveMessageRequest().withQueueUrl(queueUrl)
                .withMessageAttributeNames("traceId");
        List<Message> messages = sqs.receiveMessage(requestForMessagesWithAttributes).getMessages();
        assertThat(messages).hasSize(1);
        return messages;
    }

}
