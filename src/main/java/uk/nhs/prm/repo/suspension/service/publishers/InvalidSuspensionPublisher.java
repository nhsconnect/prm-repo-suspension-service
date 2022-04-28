package uk.nhs.prm.repo.suspension.service.publishers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InvalidSuspensionPublisher {
    private final String invalidSuspensionSnsTopicArn;
    private final String nonSensitiveInvalidSuspensionSnsTopicArn;
    private final MessagePublisher messagePublisher;

    public InvalidSuspensionPublisher(MessagePublisher messagePublisher, @Value("${aws.invalidSuspensionSnsTopicArn}") String invalidSuspensionSnsTopicArn,
                                      @Value("${aws.nonSensitiveInvalidSuspensionSnsTopicArn}") String nonSensitiveInvalidSuspensionSnsTopicArn) {
        this.messagePublisher = messagePublisher;
        this.invalidSuspensionSnsTopicArn = invalidSuspensionSnsTopicArn;
        this.nonSensitiveInvalidSuspensionSnsTopicArn = nonSensitiveInvalidSuspensionSnsTopicArn;
    }

    public void sendMessage(String message) {
        messagePublisher.sendMessage(this.invalidSuspensionSnsTopicArn, message);
    }

    public void sendNonSensitiveMessage(String nonSensitiveInvalidSuspensionMessage) {
        messagePublisher.sendMessage(this.nonSensitiveInvalidSuspensionSnsTopicArn, nonSensitiveInvalidSuspensionMessage);
    }
}

