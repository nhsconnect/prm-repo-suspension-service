package uk.nhs.prm.repo.suspension.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuditMessage {

    private final String nemsMessageId;
    private final String messageStatus;

    public AuditMessage(@JsonProperty("nemsMessageId") String nemsMessageId, @JsonProperty("messageStatus") String messageStatus) {
        this.nemsMessageId = nemsMessageId;
        this.messageStatus = messageStatus;
    }
}
