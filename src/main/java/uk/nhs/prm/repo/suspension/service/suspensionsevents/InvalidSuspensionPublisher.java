package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InvalidSuspensionPublisher {
    private final String invalidSuspensionSnsTopicArn;
    private final MessagePublisher messagePublisher;

    public InvalidSuspensionPublisher(MessagePublisher messagePublisher, @Value("${aws.invalidSuspensionSnsTopicArn}") String invalidSuspensionSnsTopicArn) {
        this.messagePublisher = messagePublisher;
        this.invalidSuspensionSnsTopicArn = invalidSuspensionSnsTopicArn;
    }

    public void sendMessage(String message) {
        messagePublisher.sendMessage(this.invalidSuspensionSnsTopicArn, message);
    }
}

