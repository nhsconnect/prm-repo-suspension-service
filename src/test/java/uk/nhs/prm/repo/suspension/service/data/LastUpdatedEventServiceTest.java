package uk.nhs.prm.repo.suspension.service.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.model.LastUpdatedEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LastUpdatedEventServiceTest {
    @Mock
    private SuspensionsDb suspensionsDb;

    private LastUpdatedEventService lastUpdatedEventService;

    String nhsNumber = "1234567890";
    String lastUpdated = "2017-11-01T15:00:33+00:00";

    @BeforeEach
    public void setUp() {
        lastUpdatedEventService = new LastUpdatedEventService(suspensionsDb);
    }

    @Test
    public void shouldReturnFalseWhenNhsNumberIsNotInDb() {
        when(suspensionsDb.getByNhsNumber(nhsNumber)).thenReturn(null);
        var eventIsOutOfDate = lastUpdatedEventService.isOutOfDate(nhsNumber, lastUpdated);

        verify(suspensionsDb).getByNhsNumber(nhsNumber);
        assertFalse(eventIsOutOfDate);
    }

    @Test
    public void shouldReturnFalseWhenDateInDbIsEarlierThanNemsMessageDate() {
        when(suspensionsDb.getByNhsNumber(nhsNumber)).thenReturn(new LastUpdatedEvent(nhsNumber, "2016-11-01T15:00:33+00:00"));
        var eventIsOutOfDate = lastUpdatedEventService.isOutOfDate(nhsNumber, lastUpdated);

        verify(suspensionsDb).getByNhsNumber(nhsNumber);
        assertFalse(eventIsOutOfDate);
    }

    @Test
    public void shouldReturnTrueWhenDateInDbIsEarlierThanNemsMessageDate() {
        when(suspensionsDb.getByNhsNumber(nhsNumber)).thenReturn(new LastUpdatedEvent(nhsNumber, "2021-11-01T15:00:33+00:00"));
        var eventIsOutOfDate = lastUpdatedEventService.isOutOfDate(nhsNumber, lastUpdated);

        verify(suspensionsDb).getByNhsNumber(nhsNumber);
        assertTrue(eventIsOutOfDate);
    }

    @Test
    public void shouldInvokeCallToDbWhenRequestedToSaveEvent(){
        lastUpdatedEventService.save(nhsNumber, lastUpdated);
        verify(suspensionsDb).save(new LastUpdatedEvent(nhsNumber, lastUpdated));
    }
}