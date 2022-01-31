package uk.nhs.prm.repo.suspension.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ManagingOrganisationUpdatedMessage {

    private final String nhsNumber;
    private final String managingOrganisationOdsCode;
    private final String nemsMessageId;

    public ManagingOrganisationUpdatedMessage(@JsonProperty("nhsNumber") String nhsNumber,
                                              @JsonProperty("managingOrganisationOdsCode") String managingOrganisationOdsCode,
                                              @JsonProperty("nemsMessageId") String nemsMessageId) {
        this.nhsNumber = nhsNumber;
        this.managingOrganisationOdsCode = managingOrganisationOdsCode;
        this.nemsMessageId = nemsMessageId;
    }
}
