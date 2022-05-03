package uk.nhs.prm.repo.suspension.service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ToggleConfig {
    @Value("${can.update.managing.organisation.to.repo}")
    private boolean canUpdateManagingOrganisationToRepo;
}
