package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MofUpdatedEventPublisherTest {

    @Mock
    private MessagePublisher messagePublisher;

    private final static String suspensionsTopicArn = "suspensionsTopicArn";

    private MofUpdatedEventPublisher mofUpdatedEventPublisher;

    @BeforeEach
    void setUp() {
        mofUpdatedEventPublisher = new MofUpdatedEventPublisher(messagePublisher, suspensionsTopicArn);
    }

    @Test
    void shouldPublishMessageToTheUnhandledTopic() {
        mofUpdatedEventPublisher.sendMessage("message");
        verify(messagePublisher).sendMessage(suspensionsTopicArn, "message");
    }
}