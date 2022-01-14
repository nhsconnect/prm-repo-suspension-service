package uk.nhs.prm.repo.suspension.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PdsAdaptorSuspensionStatusResponse {
        private final Boolean isSuspended;
        private final String currentOdsCode;
        private final String managingOrganisation;
        private final String recordETag;


        public PdsAdaptorSuspensionStatusResponse(@JsonProperty("isSuspended") Boolean isSuspended, @JsonProperty("currentOdsCode")
                String currentOdsCode, @JsonProperty("managingOrganisation") String managingOrganisation, @JsonProperty("recordETag")String recordETag) {
                this.isSuspended = isSuspended;
                this.currentOdsCode = currentOdsCode;
                this.managingOrganisation = managingOrganisation;
                this.recordETag = recordETag;
        }
}
