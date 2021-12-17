package uk.nhs.prm.repo.suspension.service.pds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@ExtendWith(MockitoExtension.class)
class PdsLookupServiceTest {

    @Mock
    private PdsLookupService pdsLookupService;

    @Test
    void isMessageSuspended(){
        //given
        String suspendedMessage = "suspendedMessage";

        //when
//        when(pdsLookupService.isSuspended(suspendedMessage)).thenReturn(true);

        //then
        assertThat(pdsLookupService.isSuspended(suspendedMessage));
    }
}