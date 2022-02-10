package uk.nhs.prm.repo.suspension.service.config;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.UUID;

@Slf4j
@Configuration
@NoArgsConstructor
public class Tracer {

    public static final String TRACE_ID = "traceId";
    public static final String NEMS_MESSAGE_ID = "nemsMessageId";

    public void setMDCContext(Message message) throws JMSException {
        clearMDCContext();
        handleTraceId(message);
        handleNemsMessageId(message);
    }

    private void handleTraceId(Message message) throws JMSException {
        if (message.getStringProperty(TRACE_ID) == null) {
            log.info("The message has no trace ID attribute, we'll create and assign one.");
            setTraceId(createTraceId());
        } else {
            log.info("The message has an existing trace ID attribute");
            setTraceId(message.getStringProperty(TRACE_ID));
        }
    }

    private String createTraceId() {
        var traceIdUUID = UUID.randomUUID().toString();
        var traceIdHex = traceIdUUID.replaceAll("-", "");
        return traceIdHex;
    }

    private void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    private void handleNemsMessageId(Message message) throws JMSException {
        if (message.getStringProperty(NEMS_MESSAGE_ID) == null) {
            log.error("The message has no NEMS message ID attribute");
        } else {
            log.info("Setting the NEMS message ID attribute");
            setNemsMessageId(message.getStringProperty(NEMS_MESSAGE_ID));
        }
    }

    private void setNemsMessageId(String nemsMessageId) {
        MDC.put(NEMS_MESSAGE_ID, nemsMessageId);
    }

    public String getNemsMessageId() {
        return MDC.get(NEMS_MESSAGE_ID);
    }

    private void clearMDCContext() {
        MDC.remove(TRACE_ID);
        MDC.remove(NEMS_MESSAGE_ID);
    }
}
