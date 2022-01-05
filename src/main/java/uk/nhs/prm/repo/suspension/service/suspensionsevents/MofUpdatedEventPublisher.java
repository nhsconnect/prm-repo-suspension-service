package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MofUpdatedEventPublisher {
    private final String mofUpdatedSnsTopicArn;
    private final MessagePublisher messagePublisher;

    public MofUpdatedEventPublisher(MessagePublisher messagePublisher, @Value("${aws.mofUpdatedSnsTopicArn}") String mofUpdatedSnsTopicArn) {
        this.messagePublisher = messagePublisher;
        this.mofUpdatedSnsTopicArn = mofUpdatedSnsTopicArn;
    }

    public void sendMessage(String message) {
        messagePublisher.sendMessage(this.mofUpdatedSnsTopicArn, message);
    }
}

