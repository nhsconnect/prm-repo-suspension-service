package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import java.util.HashMap;

public class SuspensionEvent {

    private HashMap<String, Object> fields;

    public SuspensionEvent(HashMap<String, Object> fields) {
        this.fields = fields;
    }

    public String nhsNumber() {
        return fields.get("nhsNumber").toString();
    }

    public String previousOdsCode() {
        return fields.get("previousOdsCode").toString();
    }

    public String nemsMessageId() {
        return fields.get("nemsMessageId").toString();
    }
}
