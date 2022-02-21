package uk.nhs.prm.repo.suspension.service.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EventOutOfDateServiceTest {
    @Mock
    private DbClient dbClient;

    private EventOutOfDateService eventOutOfDateService;

    String nhsNumber = "1234567890";

    @BeforeEach
    public void setUp() {
        eventOutOfDateService = new EventOutOfDateService(dbClient);
    }

    @Test
    public void shouldCallDbClientAndGetTimestampForAnNhsNumber() {
//        when(dbClient.getItem(nhsNumber)).thenReturn(GetItemResponse);
        boolean isOutOfDate = eventOutOfDateService.checkIfEventIsOutOfDate(nhsNumber);
        assertTrue(isOutOfDate);
    }

}