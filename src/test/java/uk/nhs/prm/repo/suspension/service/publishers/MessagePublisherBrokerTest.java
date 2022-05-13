package uk.nhs.prm.repo.suspension.service.publishers;

import org.assertj.core.api.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import uk.nhs.prm.repo.suspension.service.config.Tracer;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.model.RepoIncomingEvent;
import uk.nhs.prm.repo.suspension.service.suspensionsevents.SuspensionEvent;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessagePublisherBrokerTest {

    @Mock
    private NotSuspendedEventPublisher notSuspendedEventPublisher;
    @Mock
    private MofUpdatedEventPublisher mofUpdatedEventPublisher;
    @Mock
    private MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    @Mock
    private InvalidSuspensionPublisher invalidSuspensionPublisher;
    @Mock
    private EventOutOfOrderPublisher eventOutOfOrderPublisher;
    @Mock
    private DeceasedPatientEventPublisher deceasedPatientEventPublisher;
    @Mock
    private RepoIncomingEventPublisher repoIncomingEventPublisher;
    @Mock
    private Tracer tracer;

    @Captor
    private ArgumentCaptor<RepoIncomingEvent> repoIncomingEventArgumentCaptor;

    @InjectMocks
    private MessagePublisherBroker messagePublisherBroker;

    private final static String NEMS_MESSAGE_ID = "some-nems-id";

    @Test
    void notSuspendedMessage() {
        messagePublisherBroker.notSuspendedMessage(NEMS_MESSAGE_ID);
        var nonSensitiveDataMessage = new NonSensitiveDataMessage(NEMS_MESSAGE_ID, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");
        verify(notSuspendedEventPublisher).sendMessage(nonSensitiveDataMessage);
    }

    @Test
    void notSyntheticMessage() {
        messagePublisherBroker.notSyntheticMessage(NEMS_MESSAGE_ID);
        var nonSensitiveDataMessage = new NonSensitiveDataMessage(NEMS_MESSAGE_ID, "NO_ACTION:NOT_SYNTHETIC_OR_SAFE_LISTED");
        verify(mofNotUpdatedEventPublisher).sendMessage(nonSensitiveDataMessage);
    }

    @Test
    void deceasedPatientMessage() {
        messagePublisherBroker.deceasedPatientMessage(NEMS_MESSAGE_ID);
        var nonSensitiveDataMessage = new NonSensitiveDataMessage(NEMS_MESSAGE_ID, "NO_ACTION:DECEASED_PATIENT");
        verify(deceasedPatientEventPublisher).sendMessage(nonSensitiveDataMessage);
    }

    @Test
    void eventOutOfOrderMessage() {
        messagePublisherBroker.eventOutOfOrderMessage(NEMS_MESSAGE_ID);
        var nonSensitiveDataMessage = new NonSensitiveDataMessage(NEMS_MESSAGE_ID, "NO_ACTION:EVENT_PROCESSED_OUT_OF_ORDER");
        verify(eventOutOfOrderPublisher).sendMessage(nonSensitiveDataMessage);
    }

    @Test
    void invalidFormattedMessage() {
        var sensitiveMessage = "sensitive-data";
        var nonSensitiveMessage = "non-sensitive-data";
        messagePublisherBroker.invalidFormattedMessage(sensitiveMessage, nonSensitiveMessage);
        verify(invalidSuspensionPublisher).sendMessage(sensitiveMessage);
        verify(invalidSuspensionPublisher).sendNonSensitiveMessage(nonSensitiveMessage);
    }

    @Test
    void mofNotUpdatedMessage() {
        messagePublisherBroker.mofNotUpdatedMessage(NEMS_MESSAGE_ID,false);
        var nonSensitiveDataMessage = new NonSensitiveDataMessage(NEMS_MESSAGE_ID, "NO_ACTION:MOF_SAME_AS_PREVIOUS_GP");
        verify(mofNotUpdatedEventPublisher).sendMessage(nonSensitiveDataMessage);
    }

    @Test
    void mofNotUpdatedMessageWithRepoStatusTrue() {
        messagePublisherBroker.mofNotUpdatedMessage(NEMS_MESSAGE_ID,true);
        var nonSensitiveDataMessage = new NonSensitiveDataMessage(NEMS_MESSAGE_ID, "NO_ACTION:MOF_SAME_AS_REPO");
        verify(mofNotUpdatedEventPublisher).sendMessage(nonSensitiveDataMessage);
    }

    @Test
    void mofUpdatedMessageForRegularUpdate() {
        messagePublisherBroker.mofUpdatedMessage(NEMS_MESSAGE_ID, "A1000", false);
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(NEMS_MESSAGE_ID, "A1000", "ACTION:UPDATED_MANAGING_ORGANISATION");
        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
    }

    @Test
    void mofUpdatedMessageForSupersededUpdate() {
        messagePublisherBroker.mofUpdatedMessage(NEMS_MESSAGE_ID, "A1000", true);
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(NEMS_MESSAGE_ID, "A1000",
                "ACTION:UPDATED_MANAGING_ORGANISATION_FOR_SUPERSEDED_PATIENT");
        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
    }

    @Test
    void repoIncomingMessage() {
        var suspensionEvent = new SuspensionEvent("NHS_NUMBER", "PREVIOUS_ODS_CODE", NEMS_MESSAGE_ID, "LAST_UPDATED_DATE");
        var afterUpdateResponse = new PdsAdaptorSuspensionStatusResponse("NHS_NUMBER", true, null,
                "REPO_ODS_CODE", "E2", false);
        messagePublisherBroker.repoIncomingMessage(afterUpdateResponse,suspensionEvent);
        verify(repoIncomingEventPublisher).sendMessage(repoIncomingEventArgumentCaptor.capture());
        var repoIncomingEventArgumentCaptorValue = repoIncomingEventArgumentCaptor.getValue();
        assertThat(repoIncomingEventArgumentCaptorValue.getNhsNumber()).isEqualTo("NHS_NUMBER");
        assertThat(repoIncomingEventArgumentCaptorValue.getNemsMessageId()).isEqualTo(NEMS_MESSAGE_ID);
        assertThat(repoIncomingEventArgumentCaptorValue.getDestinationGp()).isEqualTo("REPO_ODS_CODE");
        assertThat(repoIncomingEventArgumentCaptorValue.getNemsEventLastUpdated()).isEqualTo("LAST_UPDATED_DATE");
        assertThat(repoIncomingEventArgumentCaptorValue.getSourceGp()).isEqualTo("PREVIOUS_ODS_CODE");
        assertThat(repoIncomingEventArgumentCaptorValue.getConversationId()).isEqualTo(tracer.getTraceId());
    }
}