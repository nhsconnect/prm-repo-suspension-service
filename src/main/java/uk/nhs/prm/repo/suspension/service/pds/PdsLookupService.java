package uk.nhs.prm.repo.suspension.service.pds;

import org.springframework.stereotype.Component;

@Component
public class PdsLookupService {
    public boolean isSuspended(String notSuspendedMessage) {
        return false;
    }
}
