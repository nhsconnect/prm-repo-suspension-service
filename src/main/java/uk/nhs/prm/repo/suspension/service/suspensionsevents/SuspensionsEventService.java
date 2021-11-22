package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionsEventService {
    private SuspensionsEventPublisher suspensionsEventPublisher;

    public void processSuspensionsEvent(String notSuspendedMessage) {
        suspensionsEventPublisher.sendMessage(notSuspendedMessage);
    }
}
