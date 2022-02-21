package uk.nhs.prm.repo.suspension.service.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.model.LastUpdatedData;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventOutOfDateServiceTest {
    @Mock
    private DbClient dbClient;

    private EventOutOfDateService eventOutOfDateService;

    String nhsNumber = "1234567890";
    String lastUpdated = "2017-11-01T15:00:33+00:00";

    @BeforeEach
    public void setUp() {
        eventOutOfDateService = new EventOutOfDateService(dbClient);
    }

    @Test
    public void shouldReturnFalseWhenNhsNumberIsNotInDb() {
        when(dbClient.getItem(nhsNumber)).thenReturn(null);
        var eventIsOutOfDate = eventOutOfDateService.checkIfEventIsOutOfDate(nhsNumber, lastUpdated);

        verify(dbClient).getItem(nhsNumber);
        assertFalse(eventIsOutOfDate);
    }

    @Test
    public void shouldReturnFalseWhenDateInDbIsEarlierThanNemsMessageDate() {
        when(dbClient.getItem(nhsNumber)).thenReturn(new LastUpdatedData(nhsNumber, "2016-11-01T15:00:33+00:00"));
        var eventIsOutOfDate = eventOutOfDateService.checkIfEventIsOutOfDate(nhsNumber, lastUpdated);

        verify(dbClient).getItem(nhsNumber);
        assertFalse(eventIsOutOfDate);
    }

    @Test
    public void shouldReturnTrueWhenDateInDbIsEarlierThanNemsMessageDate() {
        when(dbClient.getItem(nhsNumber)).thenReturn(new LastUpdatedData(nhsNumber, "2021-11-01T15:00:33+00:00"));
        var eventIsOutOfDate = eventOutOfDateService.checkIfEventIsOutOfDate(nhsNumber, lastUpdated);

        verify(dbClient).getItem(nhsNumber);
        assertTrue(eventIsOutOfDate);
    }

}