package uk.nhs.prm.repo.suspension.service.config;

import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@NoArgsConstructor
public class Tracer {

    public String createTraceId() {
        var traceIdUUID = UUID.randomUUID().toString();
        var traceIdHex = traceIdUUID.replaceAll("-", "");
        return traceIdHex;
    }

    public void setTraceId(String traceId) {
        MDC.put("traceId", traceId);
    }

    public String getTraceId() {
        return MDC.get("traceId");
    }
}
