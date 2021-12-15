package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        String notSuspendedMessage = "notSuspendedMessage";
        when(pdsLookupService.isSuspended(notSuspendedMessage)).thenReturn(false);

        suspensionsEventProcessor.processSuspensionEvent(notSuspendedMessage);

        verify(notSuspendedEventPublisher).sendMessage(notSuspendedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspended(){
        String suspendedMessage = "suspendedMessage";
        when(pdsLookupService.isSuspended(suspendedMessage)).thenReturn(true);

        suspensionsEventProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(suspendedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

}
