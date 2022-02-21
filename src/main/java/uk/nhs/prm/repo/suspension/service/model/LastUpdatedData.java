package uk.nhs.prm.repo.suspension.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LastUpdatedData {
    private String nhsNumber;
    private String lastUpdated;
}
