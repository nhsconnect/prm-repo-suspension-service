package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;


@Component
public class NotSuspendedEventPublisher {
    private final String suspensionsSnsTopicArn;
    private final MessagePublisher messagePublisher;

    public NotSuspendedEventPublisher(MessagePublisher messagePublisher, @Value("${aws.notSuspendedSnsTopicArn}") String suspensionsSnsTopicArn) {
        this.messagePublisher = messagePublisher;
        this.suspensionsSnsTopicArn = suspensionsSnsTopicArn;
    }

    public void sendMessage(NonSensitiveDataMessage message) {
        messagePublisher.sendMessage(this.suspensionsSnsTopicArn, message.toJsonString());
    }
}
