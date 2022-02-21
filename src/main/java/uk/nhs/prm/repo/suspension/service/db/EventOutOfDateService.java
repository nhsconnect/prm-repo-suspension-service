package uk.nhs.prm.repo.suspension.service.db;

import org.springframework.stereotype.Service;

@Service
public class EventOutOfDateService {

    private final DbClient dbClient;

    public EventOutOfDateService (DbClient dbClient){
        this.dbClient = dbClient;
    }

    public boolean checkIfEventIsOutOfDate(String nhsNumber) {
        dbClient.getItem(nhsNumber);
        return true;
    }
}
