package uk.nhs.prm.repo.suspension.service.metrics.healthprobes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import uk.nhs.prm.repo.suspension.service.metrics.AppConfig;

@Component
@Slf4j
public class NotSuspendedSqsHealthProbe implements HealthProbe {
    private final AppConfig config;
    public NotSuspendedSqsHealthProbe(AppConfig config) {
        this.config = config;
    }

    @Override
    public boolean isHealthy() {
        try {
            var sqsClient = SqsClient.create();
            var queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName()).build()).queueUrl();
            sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder().queueUrl(queueUrl).build());
            return true;
        } catch (RuntimeException exception) {
            log.info("Failed to query SQS queue: " + queueName(), exception);
            return false;
        }
    }

    private String queueName() {
        return config.notSuspendedQueueName();
    }
}
