package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SuspensionsEventServiceTest {

    @InjectMocks
    private SuspensionsEventService suspensionsEventService;

    @Mock
    private SuspensionsEventPublisher suspensionsEventPublisher;

    @Test
    void shouldPublishNonSuspensionsToTheUnhandledQueue() {
        String notSuspendedMessage = "notSuspendedMessage";
        suspensionsEventService.processSuspensionsEvent(notSuspendedMessage);
        verify(suspensionsEventPublisher).sendMessage("notSuspendedMessage");
    }
}
