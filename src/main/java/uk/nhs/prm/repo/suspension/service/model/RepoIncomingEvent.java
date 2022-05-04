package uk.nhs.prm.repo.suspension.service.model;

import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionEvent;

@Data
@AllArgsConstructor
public class RepoIncomingEvent {
    private String nhsNumber; //PDS Response
    private String destinationGp; //PDS Response Managing Organisation
    private String sourceGp; //Suspension Event - Previous GP
    private String nemsMessageID; //Suspension Event - NEMS Message ID
    private String conversationId; //Generate
    private String nemsEventLastUpdated; //Suspension Event - LAST UPDATED

    public RepoIncomingEvent(PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse, SuspensionEvent suspensionEvent, String conversationId) {
        this.nhsNumber = pdsAdaptorSuspensionStatusResponse.getNhsNumber();
        this.destinationGp = pdsAdaptorSuspensionStatusResponse.getManagingOrganisation();
        this.sourceGp = suspensionEvent.getPreviousOdsCode();
        this.nemsMessageID = suspensionEvent.getNemsMessageId();
        this.conversationId = conversationId;
        this.nemsEventLastUpdated = suspensionEvent.lastUpdated();
    }

    public String toJsonString() {
        return new GsonBuilder().disableHtmlEscaping().create()
                .toJson(this);
    }
}
