package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.db.EventOutOfDateService;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.IntermittentErrorPdsException;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class SuspensionMessageProcessorTest {

    private SuspensionMessageProcessor suspensionMessageProcessor;

    private MessageProcessExecution messageProcessExecution;

    @Mock
    private NotSuspendedEventPublisher notSuspendedEventPublisher;

    @Mock
    private MofUpdatedEventPublisher mofUpdatedEventPublisher;

    @Mock
    private MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;

    @Mock
    private EventOutOfDateService eventOutOfDateService;

    @Mock
    private EventOutOfDatePublisher eventOutOfDatePublisher;

    @Mock
    private InvalidSuspensionPublisher invalidSuspensionPublisher;

    @Mock
    private ConcurrentThreadLock concurrentThreadLock;

    private static final String nemsMessageId = "A6FBE8C3-9144-4DDD-BFFE-B49A96456B29";

    @Mock
    private PdsService pdsService;

    private static final String NHS_NUMBER = "9692294951";

    @BeforeEach
    public void setUp() {
        messageProcessExecution = new MessageProcessExecution(notSuspendedEventPublisher, mofUpdatedEventPublisher,
                mofNotUpdatedEventPublisher, invalidSuspensionPublisher, eventOutOfDatePublisher,
                pdsService, eventOutOfDateService, new SuspensionEventParser(), concurrentThreadLock);
        suspensionMessageProcessor = new SuspensionMessageProcessor(messageProcessExecution);
        setField(suspensionMessageProcessor, "initialIntervalMillis", 1);
        setField(suspensionMessageProcessor, "maxAttempts", 5);
        setField(suspensionMessageProcessor, "multiplier", 2.0);
    }

    @Test
    void shouldRetryAsMaxAttemptNumber() {
        var sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER)).thenThrow(IntermittentErrorPdsException.class);


        Assertions.assertThrows(IntermittentErrorPdsException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        int numberOfInvocations = 5;

        verify(pdsService, times(numberOfInvocations)).isSuspended(NHS_NUMBER);

    }

    @Test
    void shouldNotRetryIfNotGetException() {
        var sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER)).thenThrow(IntermittentErrorPdsException.class)
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, false, null, null, ""));

        suspensionMessageProcessor.process(sampleMessage);

        int numberOfInvocations = 2;

        verify(pdsService, times(numberOfInvocations)).isSuspended(NHS_NUMBER);
    }

    @Test
    void shouldNotRetryWhenGotInvalidPdsRequestException() {
        var sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER)).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        verify(pdsService, times(1)).isSuspended(NHS_NUMBER);
    }

    @Test
    void shouldPutInvalidBogusMessageOnInvalidSuspensionMessageOnDLQ() {
        var sampleMessage = "invalid-bogus";

        Assertions.assertThrows(InvalidSuspensionMessageException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        verify(invalidSuspensionPublisher).sendMessage(sampleMessage);
        verify(invalidSuspensionPublisher).sendNonSensitiveMessage(sampleMessage);
    }

    @Test
    void shouldNotProcessMessagesWhichAreNotInCorrectFormat() {
        var message = "invalid message";
        Assertions.assertThrows(Exception.class, () -> suspensionMessageProcessor.process(message));
    }
}