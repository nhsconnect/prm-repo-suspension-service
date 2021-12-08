package uk.nhs.prm.repo.suspension.service.config;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionsEventListener;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionsEventService;

import javax.jms.JMSException;
import javax.jms.Session;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SqsClientSpringConfiguration {
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.create();
    }
}
