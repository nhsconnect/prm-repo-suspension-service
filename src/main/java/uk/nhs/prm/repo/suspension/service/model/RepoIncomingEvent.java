package uk.nhs.prm.repo.suspension.service.model;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionEvent;

@Data
@AllArgsConstructor
public class RepoIncomingEvent {
    @Expose
    private String nhsNumber;

    @Expose
    private String destinationGp;

    @Expose
    private String sourceGp;

    @Expose
    private String nemsMessageId;

    @Expose
    private String conversationId;

    @Expose(serialize = false)
    private String nemsEventLastUpdated;

    public RepoIncomingEvent(PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse, SuspensionEvent suspensionEvent, String conversationId) {
        this.nhsNumber = pdsAdaptorSuspensionStatusResponse.getNhsNumber();
        this.destinationGp = pdsAdaptorSuspensionStatusResponse.getManagingOrganisation();
        this.sourceGp = suspensionEvent.getPreviousOdsCode();
        this.nemsMessageId = suspensionEvent.getNemsMessageId();
        this.conversationId = conversationId;
        this.nemsEventLastUpdated = suspensionEvent.lastUpdated();
    }

    public String toJsonString() {
        return new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create()
                .toJson(this);
    }
}
