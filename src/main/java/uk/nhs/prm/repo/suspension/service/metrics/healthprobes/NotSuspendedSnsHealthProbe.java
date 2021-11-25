package uk.nhs.prm.repo.suspension.service.metrics.healthprobes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import uk.nhs.prm.repo.suspension.service.metrics.AppConfig;

@Slf4j
@Component
public class NotSuspendedSnsHealthProbe implements HealthProbe {
    private final AppConfig config;

    public NotSuspendedSnsHealthProbe(AppConfig config) {
        this.config = config;
    }

    @Override
    public boolean isHealthy() {
        try {
            SnsClient snsClient = SnsClient.create();
            snsClient.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(config.notSuspendedSnsTopicArn()).build());
            return true;
        } catch (RuntimeException exception) {
            log.info("Failed to query SNS topic: " + config.notSuspendedSnsTopicArn(), exception);
            return false;
        }
    }
}
