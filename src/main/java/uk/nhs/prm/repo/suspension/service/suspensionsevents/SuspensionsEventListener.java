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

        try {
            tracer.setTraceId(message.getStringProperty("traceId"));
            String payload = ((TextMessage) message).getText();
            suspensionsEventService.processSuspensionsEvent(payload);
            message.acknowledge();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
