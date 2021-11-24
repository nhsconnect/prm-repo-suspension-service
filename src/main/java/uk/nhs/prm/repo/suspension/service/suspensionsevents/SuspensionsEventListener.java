package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.prm.repo.suspension.service.config.Tracer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Slf4j
@RequiredArgsConstructor
public class SuspensionsEventListener implements MessageListener {

    private final SuspensionsEventService suspensionsEventService;
    private final Tracer tracer;

    @Override
    public void onMessage(Message message) {
        String traceId = tracer.createTraceId();
        tracer.setTraceId(traceId);
        String payload = null;
        try {
            payload = ((TextMessage) message).getText();
            suspensionsEventService.processSuspensionsEvent(payload);
            message.acknowledge();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
