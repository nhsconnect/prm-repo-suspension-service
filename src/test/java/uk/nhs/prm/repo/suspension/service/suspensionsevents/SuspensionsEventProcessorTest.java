package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsLookupService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SuspensionsEventProcessorTest {

    @InjectMocks
    private SuspensionsEventProcessor suspensionsEventProcessor;

    @Mock
    private NotSuspendedEventPublisher notSuspendedEventPublisher;

    @Mock
    private MofUpdatedEventPublisher mofUpdatedEventPublisher;

    @Mock
    private PdsLookupService pdsLookupService;


    @Test
    void shouldPublishASuspensionMessageToNotSuspendedSNSTopicWhenPatientIsNotCurrentlySuspended(){
        String notSuspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(false, "null", "", "");
        when(pdsLookupService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionsEventProcessor.processSuspensionEvent(notSuspendedMessage);

        verify(notSuspendedEventPublisher).sendMessage(notSuspendedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspended(){
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(true, "12345","", "");
        when(pdsLookupService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionsEventProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(suspendedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirme(){
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(true, "12345","", "");
        when(pdsLookupService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionsEventProcessor.processSuspensionEvent(sampleMessage);

        verify(mofUpdatedEventPublisher).sendMessage(sampleMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

    @Test
    void shouldNotProcessMessagesWhichHasNotProperNhsNumber(){
        String message = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"invalid\"}\",\"environment\":\"local\"}";
        Assertions.assertThrows(Exception.class, () -> {
            suspensionsEventProcessor.processSuspensionEvent(message);
        });
    }

    @Test
    void shouldNotProcessMessagesWhichHaveNoNhsNumber(){
        String message = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"environment\":\"local\"}";
        Assertions.assertThrows(Exception.class, () -> {
            suspensionsEventProcessor.processSuspensionEvent(message);
        });
    }

    @Test
    void shouldNotProcessMessagesWhichAreNotInCorrectFormat(){
        String message = "invalid message";
        Assertions.assertThrows(Exception.class, () -> {
            suspensionsEventProcessor.processSuspensionEvent(message);
        });
    }
}
