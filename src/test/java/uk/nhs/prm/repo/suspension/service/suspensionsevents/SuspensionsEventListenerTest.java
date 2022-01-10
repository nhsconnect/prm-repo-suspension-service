package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import ch.qos.logback.classic.Level;
import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.config.Tracer;
import uk.nhs.prm.repo.suspension.service.logging.TestLogAppender;

import javax.jms.JMSException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static uk.nhs.prm.repo.suspension.service.logging.TestLogAppender.addTestLogAppender;

@ExtendWith(MockitoExtension.class)
public class SuspensionsEventListenerTest {

        @Mock
        private SuspensionsEventProcessor suspensionsEventProcessor;
        @Mock
        private Tracer tracer;

        @InjectMocks
        private SuspensionsEventListener suspensionsEventListener;

        @Test
        void shouldCallNemsEventServiceWithReceivedMessage() throws JMSException {
            String payload = "payload";
            SQSTextMessage message = spy(new SQSTextMessage(payload));

            suspensionsEventListener.onMessage(message);
            verify(suspensionsEventProcessor).processSuspensionEvent(payload);
            verify(message).acknowledge();
        }

        @Test
        void shouldSwallowAndLogExceptionsAsErrorsInProcessingWithoutAcknowledgingMessage() throws JMSException {
            var logged = addTestLogAppender();
            var exception = new IllegalStateException("some exception");
            var message = spy(new SQSTextMessage("bob"));

            doThrow(exception).when(suspensionsEventProcessor).processSuspensionEvent(any());

            suspensionsEventListener.onMessage(message);

            assertThat(logged.findLoggedEvent("Failure to handle message").getThrowableProxy().getMessage()).isEqualTo("some exception");
            verify(message, never()).acknowledge();
        }
    }
