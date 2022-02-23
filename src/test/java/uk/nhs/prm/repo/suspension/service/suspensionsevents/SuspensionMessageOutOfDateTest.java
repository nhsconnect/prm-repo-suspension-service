package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.db.EventOutOfDateService;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class SuspensionMessageOutOfDateTest {
    private SuspensionMessageProcessor suspensionMessageProcessor;

    @Mock
    private EventOutOfDateService eventOutOfDateService;
    @Mock
    private NotSuspendedEventPublisher notSuspendedEventPublisher;
    @Mock
    private MofUpdatedEventPublisher mofUpdatedEventPublisher;
    @Mock
    private MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    @Mock
    private InvalidSuspensionPublisher invalidSuspensionPublisher;
    @Mock
    private EventOutOfDatePublisher eventOutOfDatePublisher;
    @Mock
    private PdsService pdsService;
    @Mock
    private ConcurrentThreadLock concurrentThreadLock;

    private static final String nemsMessageId = "A6FBE8C3-9144-4DDD-BFFE-B49A96456B29";
    private static final String nhsNumber = "9692294951";

    @BeforeEach
    public void setUp() {
        var messageProcessExecution = new MessageProcessExecution(notSuspendedEventPublisher, mofUpdatedEventPublisher,
                mofNotUpdatedEventPublisher, invalidSuspensionPublisher, eventOutOfDatePublisher,
                pdsService, eventOutOfDateService, new SuspensionEventParser(), concurrentThreadLock);
        suspensionMessageProcessor = new SuspensionMessageProcessor(messageProcessExecution);

        setField(suspensionMessageProcessor, "initialIntervalMillis", 1);
        setField(suspensionMessageProcessor, "maxAttempts", 5);
        setField(suspensionMessageProcessor, "multiplier", 2.0);
    }

    @Test
    void shouldSendOutOfDateNemsMessagesToEventOutOfDateQueue() {
        var lastUpdatedDate = "2017-11-01T15:00:33+00:00";
        var suspendedMessage = "{\"lastUpdated\":\"" + lastUpdatedDate + "\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"" + nhsNumber + "\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";
        when(eventOutOfDateService.checkIfEventIsOutOfDate(nhsNumber, lastUpdatedDate)).thenReturn(true);

        suspensionMessageProcessor.process(suspendedMessage);

        verify(eventOutOfDateService).checkIfEventIsOutOfDate(nhsNumber, lastUpdatedDate);
        verify(eventOutOfDatePublisher).sendMessage(new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:EVENT_PROCESSED_OUT_OF_ORDER"));
    }
}
