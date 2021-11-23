package uk.nhs.prm.repo.suspension.service.notsuspendedevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotSuspendedEventService {

    public void processSuspensionsEvent(String notSuspendedMessage) {
        log.info("Message in the queue:" + notSuspendedMessage );
        //TODO
    }
}
