package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.data.LastUpdatedEventService;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageProcessLastUpdatedEvent {
    private MessageProcessExecution messageProcessExecution;

    @Mock
    private LastUpdatedEventService lastUpdatedEventService;
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
    private static final String lastUpdated = "2017-11-01T15:00:33+00:00";
    private static final String suspendedMessage = "{\"lastUpdated\":\"" + lastUpdated + "\"," +
            "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
            "\"eventType\":\"SUSPENSION\"," +
            "\"nhsNumber\":\"" + nhsNumber + "\"," +
            "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
            "\"environment\":\"local\"}";

    @BeforeEach
    public void setUp() {
        messageProcessExecution = new MessageProcessExecution(notSuspendedEventPublisher, mofUpdatedEventPublisher,
                mofNotUpdatedEventPublisher, invalidSuspensionPublisher, eventOutOfDatePublisher,
                pdsService, lastUpdatedEventService, new SuspensionEventParser(), concurrentThreadLock);
    }

    @Test
    void shouldSendOutOfDateNemsMessagesToEventOutOfDateQueue() {
        when(lastUpdatedEventService.isOutOfDate(nhsNumber, lastUpdated)).thenReturn(true);

        messageProcessExecution.run(suspendedMessage);

        verify(lastUpdatedEventService).isOutOfDate(nhsNumber, lastUpdated);
        verify(eventOutOfDatePublisher).sendMessage(new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:EVENT_PROCESSED_OUT_OF_ORDER"));
    }

    @Disabled("Pending investigation on integration test failing when this functionality is on")
    @Test
    void shouldSaveRecordIfisNotOutOfDate() {
        mockMofDependencies();
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "PREVIOUS_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION");
        when(lastUpdatedEventService.isOutOfDate(nhsNumber, lastUpdated)).thenReturn(false);

        messageProcessExecution.run(suspendedMessage);

        verify(lastUpdatedEventService).isOutOfDate(nhsNumber, lastUpdated);
        verify(eventOutOfDatePublisher, never()).sendMessage(any());
        verify(lastUpdatedEventService).save(nhsNumber, lastUpdated);
        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
    }

    private void mockMofDependencies() {
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(nhsNumber, true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(nhsNumber, true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended(nhsNumber)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(nhsNumber, "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);
    }
}
