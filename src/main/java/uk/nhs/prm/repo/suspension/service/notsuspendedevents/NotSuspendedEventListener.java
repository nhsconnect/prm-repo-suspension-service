package uk.nhs.prm.repo.suspension.service.notsuspendedevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.prm.repo.suspension.service.config.Tracer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Slf4j
@RequiredArgsConstructor
public class NotSuspendedEventListener implements MessageListener {

    private final NotSuspendedEventService notSuspendedEventService;
    private final Tracer tracer;

    @Override
    public void onMessage(Message message) {
        log.info("RECEIVED: Not Suspended Event Message");
        if(tracer.getTraceId().isEmpty()){
            tracer.createTraceId();
        }
        try {
            String payload = ((TextMessage) message).getText();
            notSuspendedEventService.processSuspensionsEvent(payload);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
