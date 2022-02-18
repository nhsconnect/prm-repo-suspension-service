package uk.nhs.prm.repo.suspension.service.config;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionMessageProcessor;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionsEventListener;

import javax.jms.Session;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SqsListenerSpringConfiguration {

    @Value("${aws.suspensionsQueueName}")
    private String suspensionsQueueName;

    @Value("${suspension.concurrency.min.max}")
    private String concurrencyMinMax;

    @Value("${suspension.concurrency.max.messages.per.task}")
    private Integer maxMessagesPerTask;

    @Value("${suspension.thread.core.pool.size}")
    private Integer threadCorePoolSize;

    @Value("${suspension.thread.max.pool.size}")
    private Integer threadMaxPoolSize;

    private final SuspensionMessageProcessor suspensionsEventProcessor;
    private final Tracer tracer;

    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        return AmazonSQSAsyncClientBuilder.defaultClient();
    }

    @Bean
    public DefaultMessageListenerContainer jmsListener(DefaultJmsListenerContainerFactory jmsListenerContainerFactory) {
        SimpleJmsListenerEndpoint simpleJmsListenerEndpoint = new SimpleJmsListenerEndpoint();
        simpleJmsListenerEndpoint.setMessageListener(new SuspensionsEventListener(suspensionsEventProcessor, tracer));
        simpleJmsListenerEndpoint.setDestination(suspensionsQueueName);
        return jmsListenerContainerFactory.createListenerContainer(simpleJmsListenerEndpoint);
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(AmazonSQSAsync amazonSQS) {
        ProviderConfiguration providerConfiguration = new ProviderConfiguration().withNumberOfMessagesToPrefetch(0);
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(providerConfiguration, amazonSQS);
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConcurrency(concurrencyMinMax);
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setTaskExecutor(createDefaultTaskExecutor());
        factory.setMaxMessagesPerTask(maxMessagesPerTask);
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }

    protected AsyncTaskExecutor createDefaultTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("SQSExecutor - ");
        threadPoolTaskExecutor.setCorePoolSize(threadCorePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(threadMaxPoolSize);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }
}
