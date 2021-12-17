package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.pds.PdsLookupService;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionsEventProcessor {
    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final PdsLookupService pdsLookupService;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;

    public void processSuspensionEvent(String suspensionMessage) {
        if(pdsLookupService.isSuspended(suspensionMessage).getIsSuspended()){
            mofUpdatedEventPublisher.sendMessage(suspensionMessage);
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
    }
}
