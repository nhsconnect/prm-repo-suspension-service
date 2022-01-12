package uk.nhs.prm.repo.suspension.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class PdsAdaptorSuspensionStatusResponse {
        private final Boolean isSuspended;
        private final Object currentOdsCode; // @todo these should be strings?
        private final Object managingOrganisation; // @todo these should be strings?
        private final String recordETag;


        public PdsAdaptorSuspensionStatusResponse(@JsonProperty("isSuspended") Boolean isSuspended, @JsonProperty("currentOdsCode")
                Object currentOdsCode, @JsonProperty("managingOrganisation") Object managingOrganisation, @JsonProperty("recordETag")String recordETag) {
                this.isSuspended = isSuspended;
                this.currentOdsCode = currentOdsCode;
                this.managingOrganisation = managingOrganisation;
                this.recordETag = recordETag;
        }
}
