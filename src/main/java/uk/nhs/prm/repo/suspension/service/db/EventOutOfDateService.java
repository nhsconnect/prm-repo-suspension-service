package uk.nhs.prm.repo.suspension.service.db;

import org.springframework.stereotype.Service;

@Service
public class EventOutOfDateService {

    private final DbClient dbClient;

    public EventOutOfDateService (DbClient dbClient){
        this.dbClient = dbClient;
    }

    public boolean checkIfEventIsOutOfDate(String nhsNumber, String lastUpdated) {
        var lastUpdatedData = dbClient.getItem(nhsNumber);
        return lastUpdatedData != null
                && isGreaterOrEqualThan(lastUpdatedData.getLastUpdated(), lastUpdated);
    }

    private boolean isGreaterOrEqualThan(String a, String b) {
        return a.compareTo(b) >= 0;
    }

}
