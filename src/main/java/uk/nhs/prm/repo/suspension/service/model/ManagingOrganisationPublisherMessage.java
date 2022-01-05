package uk.nhs.prm.repo.suspension.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ManagingOrganisationPublisherMessage {

    private final String nhsNumber;
    private final String managingOrgainsationOdsCode;

    public ManagingOrganisationPublisherMessage(@JsonProperty("nhsNumber") String nhsNumber, @JsonProperty("managingOrgainsationOdsCode")
            String managingOrgainsationOdsCode) {
        this.nhsNumber = nhsNumber;
        this.managingOrgainsationOdsCode = managingOrgainsationOdsCode;
    }
}
