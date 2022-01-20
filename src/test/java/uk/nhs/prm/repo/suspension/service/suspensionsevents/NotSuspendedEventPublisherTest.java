package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotSuspendedEventPublisherTest {

    @Mock
    private MessagePublisher messagePublisher;

    private final static String suspensionsTopicArn = "suspensionsTopicArn";

    private NotSuspendedEventPublisher notSuspendedEventPublisher;

    @BeforeEach
    void setUp() {
        notSuspendedEventPublisher = new NotSuspendedEventPublisher(messagePublisher, suspensionsTopicArn);
    }

    @Test
    void shouldPublishMessageToTheUnhandledTopic() {
        notSuspendedEventPublisher.sendMessage("message");
        verify(messagePublisher).sendMessage(suspensionsTopicArn, "message");
    }
}