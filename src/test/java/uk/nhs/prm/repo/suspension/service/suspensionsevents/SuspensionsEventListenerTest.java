package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.config.Tracer;

import javax.jms.JMSException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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

    }
