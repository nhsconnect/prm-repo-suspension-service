package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@TestConfiguration
public class LocalStackAwsConfig {

    @Autowired
    private AmazonSQSAsync amazonSQSAsync;

    @Autowired
    private SnsClient snsClient;

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${aws.notSuspendedQueueName}")
    private String notSuspendedQueueName;


    @Bean
    public static AmazonSQSAsync amazonSQSAsync() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("FAKE", "FAKE")))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localstack:4566", "eu-west-2"))
                .build();
    }

    @Bean
    public static SnsClient snsClient() {
        return SnsClient.builder()
                .endpointOverride(URI.create("http://localstack:4566"))
                .region(Region.EU_WEST_2)
                .credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return "FAKE";
                    }

                    @Override
                    public String secretAccessKey() {
                        return "FAKE";
                    }
                }))
                .build();
    }

    @PostConstruct
    public void setupTestQueuesAndTopics() {
        amazonSQSAsync.createQueue(suspensionsQueueName);
        CreateQueueResult notSuspendedQueue = amazonSQSAsync.createQueue(notSuspendedQueueName);
        CreateTopicResponse topic = snsClient.createTopic(CreateTopicRequest.builder().name("test_not_suspended_topic").build());

        createSnsTestReceiverSubscription(topic, getQueueArn(notSuspendedQueue.getQueueUrl()));
    }


    private void createSnsTestReceiverSubscription(CreateTopicResponse topic, String queueArn) {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("RawMessageDelivery", "True");
        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .topicArn(topic.topicArn())
                .protocol("sqs")
                .endpoint(queueArn)
                .attributes(attributes)
                .build();

        snsClient.subscribe(subscribeRequest);
    }

    private String getQueueArn(String queueUrl) {
        GetQueueAttributesResult queueAttributes = amazonSQSAsync.getQueueAttributes(queueUrl, List.of("QueueArn"));
        return queueAttributes.getAttributes().get("QueueArn");
    }
}

