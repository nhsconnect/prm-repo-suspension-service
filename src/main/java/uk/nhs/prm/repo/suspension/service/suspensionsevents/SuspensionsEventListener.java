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

    private final SuspensionsEventProcessor suspensionsEventProcessor;
    private final Tracer tracer;

    @Override
    public void onMessage(Message message) {
        log.info("RECEIVED: Suspensions Event Message");
        try {
            setTraceId(message);
            var payload = ((TextMessage) message).getText();
            suspensionsEventProcessor.processSuspensionEvent(payload);
            message.acknowledge();
        } catch (Exception e) {
            log.error("Failure to handle message", e);
        }
    }

    private void setTraceId(Message message) throws JMSException {
        if(message.getStringProperty("traceId")==null){
            log.info("The message has no trace id attribute, we'll create and assign one.");
            tracer.setTraceId(tracer.createTraceId());
        }else{
            log.info("The message has a trace id attribute");
            tracer.setTraceId(message.getStringProperty("traceId"));
        }
    }
}
