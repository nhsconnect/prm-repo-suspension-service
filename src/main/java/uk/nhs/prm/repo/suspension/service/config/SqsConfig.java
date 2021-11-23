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
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionsEventListener;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionsEventService;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SqsConfig {

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${aws.notSuspendedQueueName}")
    private String notSuspendedQueueName;



    private final SuspensionsEventService suspensionsEventService;
    private final Tracer tracer;

    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        return AmazonSQSAsyncClientBuilder.defaultClient();
    }

    @Bean
    public SQSConnection createConnection(AmazonSQSAsync amazonSQSAsync) throws JMSException {
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), amazonSQSAsync);
        return connectionFactory.createConnection();
    }

    @Bean
    public Session createSuspensionsListeners(SQSConnection connection) throws JMSException, InterruptedException {
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        log.info("suspensions event queue name : {}", suspensionsQueueName);
        MessageConsumer consumer = session.createConsumer(session.createQueue(suspensionsQueueName));

        consumer.setMessageListener(new SuspensionsEventListener(suspensionsEventService, tracer));

        connection.start();

        // TODO: check if we can get rid of this
        Thread.sleep(1000);

        return session;
    }

    @Bean
    public Session createNotSuspendedListeners(SQSConnection connection) throws JMSException, InterruptedException {
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        log.info("not suspended event queue name : {}", notSuspendedQueueName);
        MessageConsumer consumer = session.createConsumer(session.createQueue(notSuspendedQueueName));

        consumer.setMessageListener(new SuspensionsEventListener(suspensionsEventService, tracer));

        connection.start();

        // TODO: check if we can get rid of this
        Thread.sleep(1000);

        return session;
    }
}
