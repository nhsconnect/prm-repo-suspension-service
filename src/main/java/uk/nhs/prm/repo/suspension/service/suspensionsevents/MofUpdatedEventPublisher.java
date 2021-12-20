package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MofUpdatedEventPublisher {
    private final String suspensionsSnsTopicArn;
    private final MessagePublisher messagePublisher;

    public MofUpdatedEventPublisher(MessagePublisher messagePublisher, @Value("${aws.mofUpdatedSnsTopicArn}") String suspensionsSnsTopicArn) {
        this.messagePublisher = messagePublisher;
        this.suspensionsSnsTopicArn = suspensionsSnsTopicArn;
    }

    public void sendMessage(String message) {
        messagePublisher.sendMessage(this.suspensionsSnsTopicArn, message);
    }
}

