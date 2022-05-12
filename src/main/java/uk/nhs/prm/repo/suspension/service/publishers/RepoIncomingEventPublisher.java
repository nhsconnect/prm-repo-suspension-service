package uk.nhs.prm.repo.suspension.service.publishers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.RepoIncomingEvent;

@Component
public class RepoIncomingEventPublisher {
    private final MessagePublisher messagePublisher;
    private final String repoIncomingSnsTopicArn;
    private final String repoIncomingAuditSnsTopicArn;


    public RepoIncomingEventPublisher(MessagePublisher messagePublisher,
                                      @Value("${aws.repoIncomingSnsTopicArn}") String repoIncomingSnsTopicArn,
                                      @Value("${aws.repoIncomingAuditSnsTopicArn}") String repoIncomingAuditSnsTopicArn) {
        this.messagePublisher = messagePublisher;
        this.repoIncomingSnsTopicArn = repoIncomingSnsTopicArn;
        this.repoIncomingAuditSnsTopicArn=repoIncomingAuditSnsTopicArn;
    }

    public void sendMessage(RepoIncomingEvent repoIncomingEvent) {
        messagePublisher.sendMessage(this.repoIncomingSnsTopicArn, repoIncomingEvent.toJsonString());
        messagePublisher.sendMessage(this.repoIncomingAuditSnsTopicArn,
                new NonSensitiveDataMessage(repoIncomingEvent.getNemsMessageId(), "ACTION:REPO-INCOMING").toJsonString());

    }
}
