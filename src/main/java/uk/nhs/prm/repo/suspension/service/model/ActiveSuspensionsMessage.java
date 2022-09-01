package uk.nhs.prm.repo.suspension.service.model;

import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ActiveSuspensionsMessage {

    private final String nhsNumber;
    private final String previousOdsCode;
    private final String nemsLastUpdatedDate;

    public String toJsonString() {
        return new GsonBuilder().disableHtmlEscaping().create().toJson(this);
    }
}
