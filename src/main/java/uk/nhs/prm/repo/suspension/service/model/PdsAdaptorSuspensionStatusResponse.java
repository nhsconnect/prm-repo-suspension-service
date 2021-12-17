package uk.nhs.prm.repo.suspension.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PdsAdaptorSuspensionStatusResponse {
        private Boolean isSuspended;
        private String currentOdsCode;
}
