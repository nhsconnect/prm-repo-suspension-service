package uk.nhs.prm.repo.suspension.service.metrics.healthprobes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import uk.nhs.prm.repo.suspension.service.metrics.AppConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class NotSuspendedSnsHealthProbe implements HealthProbe {
    private final AppConfig config;
    private final SnsClient snsClient;
    List<int[]> list = new ArrayList<>();

    @Autowired
    public NotSuspendedSnsHealthProbe(AppConfig config, SnsClient snsClient) {
        this.config = config;
        this.snsClient = snsClient;
    }

    @Override
    public boolean isHealthy() {
        try {
            list.add(new int[10000000]);

            snsClient.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(config.notSuspendedSnsTopicArn()).build());
            return true;
        } catch (RuntimeException exception) {
            log.info("Failed to query SNS topic: " + config.notSuspendedSnsTopicArn(), exception);
            return false;
        }
    }
}
