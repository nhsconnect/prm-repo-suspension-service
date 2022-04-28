package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.data.LastUpdatedEventService;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;
import uk.nhs.prm.repo.suspension.service.publishers.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageProcessLastUpdatedEventTest {
    private MessageProcessExecution messageProcessExecution;

    @Mock
    private MessagePublisherBroker messagePublisherBroker;
    @Mock
    private LastUpdatedEventService lastUpdatedEventService;
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
        messageProcessExecution = new MessageProcessExecution(messagePublisherBroker,
                pdsService, lastUpdatedEventService, new SuspensionEventParser(), concurrentThreadLock);
    }

    @Test
    void shouldSendOutOfOrderNemsMessagesToEventOutOrderQueue() {
        when(lastUpdatedEventService.isOutOfOrder(nhsNumber, lastUpdated)).thenReturn(true);

        messageProcessExecution.run(suspendedMessage);

        verify(lastUpdatedEventService).isOutOfOrder(nhsNumber, lastUpdated);
        verify(messagePublisherBroker).eventOutOfOrderMessage(nemsMessageId);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldSaveRecordIfisNotOutOfOrder() {
        mockMofDependencies();
        when(lastUpdatedEventService.isOutOfOrder(nhsNumber, lastUpdated)).thenReturn(false);

        messageProcessExecution.run(suspendedMessage);

        verify(lastUpdatedEventService).isOutOfOrder(nhsNumber, lastUpdated);
        verify(lastUpdatedEventService).save(nhsNumber, lastUpdated);
        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, "PREVIOUS_ODS_CODE", false);

    }

    private void mockMofDependencies() {
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(nhsNumber, true, null, null, "", false);
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(nhsNumber, true, null, "PREVIOUS_ODS_CODE", "", false);

        when(pdsService.isSuspended(nhsNumber)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(nhsNumber, "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);
    }
}
