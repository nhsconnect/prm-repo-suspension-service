package uk.nhs.prm.repo.suspension.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ManagingOrganisationPublisherMessage {

    private final String nhsNumber;
    private final String managingOrganisationOdsCode;

    public ManagingOrganisationPublisherMessage(@JsonProperty("nhsNumber") String nhsNumber, @JsonProperty("managingOrganisationOdsCode")
            String managingOrganisationOdsCode) {
        this.nhsNumber = nhsNumber;
        this.managingOrganisationOdsCode = managingOrganisationOdsCode;
    }
}
