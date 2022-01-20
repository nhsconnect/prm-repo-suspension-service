package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class SuspensionsMessageProcessorTest {

    private SuspensionMessageProcessor suspensionMessageProcessor;

    @Mock
    private NotSuspendedEventPublisher notSuspendedEventPublisher;

    @Mock
    private MofUpdatedEventPublisher mofUpdatedEventPublisher;

    @Mock
    private MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;

    @Mock
    private PdsService pdsService;

    @BeforeEach
    public void setUp() {
        suspensionMessageProcessor = new SuspensionMessageProcessor(notSuspendedEventPublisher, mofUpdatedEventPublisher,
                mofNotUpdatedEventPublisher, pdsService, new SuspensionEventParser());
    }

    @Test
    void shouldUpdateMofForSyntheticPatientsWhenToggleIsOn() {
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "true");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "969");
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        PdsAdaptorSuspensionStatusResponse pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        String messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"PREVIOUS_ODS_CODE\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldUpdateMofForNonSyntheticPatientsWhenToggleIsOff(){
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "false");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "999");
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        PdsAdaptorSuspensionStatusResponse pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        String messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"PREVIOUS_ODS_CODE\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldUpdateMofForSyntheticPatientsWhenToggleIsOff(){
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "false");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "969");
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        PdsAdaptorSuspensionStatusResponse pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        String messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"PREVIOUS_ODS_CODE\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishASuspensionMessageToNotSuspendedSNSTopicWhenPatientIsNotCurrentlySuspended() {
        String notSuspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", false, "null", "", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionMessageProcessor.processSuspensionEvent(notSuspendedMessage);

        verify(notSuspendedEventPublisher).sendMessage(notSuspendedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspended() throws JsonProcessingException {
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"ORIGINAL_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, "12345", "NEW_ODS_CODE", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "ORIGINAL_ODS_CODE", "")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        String messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"NEW_ODS_CODE\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspendedAndSuperseded() throws JsonProcessingException {
        final var ORIGINAL_NHS_NUMBER = "9692294951";
        final var SUPERSEDED_NHS_NUMBER = "1234567890";

        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"ORIGINAL_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"" + ORIGINAL_NHS_NUMBER + "\"}\"," +
                "\"environment\":\"local\"}";
        var pdsAdaptorSuspensionStatusResponse = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, "12345", "NEW_ODS_CODE", "ORIGINAL_E_TAG");
        var pdsAdaptorSuspensionStatusResponseSuperseded = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, null, "NEW_ODS_CODE", "SUPERSEDED_E_TAG");
        when(pdsService.isSuspended(ORIGINAL_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.isSuspended(SUPERSEDED_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponseSuperseded);
        when(pdsService.updateMof(SUPERSEDED_NHS_NUMBER, "ORIGINAL_ODS_CODE", "SUPERSEDED_E_TAG")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        String messageJson = "{\"nhsNumber\":\"" + SUPERSEDED_NHS_NUMBER + "\"," +
                "\"managingOrganisationOdsCode\":\"NEW_ODS_CODE\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldUpdateMofWhenPatientIsConfirmedSuspended() {
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, "12345", "", "W/\"5\"");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "B85612", "W/\"5\"")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);
        verify(pdsService).updateMof("9692294951", "B85612", "W/\"5\"");
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientConfirmedSuspendedWhenPdsAlreadyHasADifferentMOFValue() throws JsonProcessingException {
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"LAST_GP_BEFORE_SUSPENSION_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorLookUpSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "EXISTING_MOF_ODS_CODE", "");

        PdsAdaptorSuspensionStatusResponse pdsAdaptorUpdateSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorLookUpSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "")).thenReturn(pdsAdaptorUpdateSuspensionStatusResponse);

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage("{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"LAST_GP_BEFORE_SUSPENSION_ODS_CODE\"}");
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

    @Test
    void shouldPublishSuspendedMessageToMofNotUpdatedSnsTopicWhenPatientMofAlreadySetToCorrectValue() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "B85612", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.processSuspensionEvent(sampleMessage);

        verify(mofNotUpdatedEventPublisher).sendMessage(sampleMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldParsePdsResponseWhenMofFieldNull() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", false, "B86041", null, "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.processSuspensionEvent(sampleMessage);

        verify(notSuspendedEventPublisher).sendMessage(sampleMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(mofNotUpdatedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldParsePdsResponseWhenCurrentOdsCodeFieldNull() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "B85612", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.processSuspensionEvent(sampleMessage);

        verify(mofNotUpdatedEventPublisher).sendMessage(sampleMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldParseSuspendedPdsResponseWhenMofFieldNull() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "B85612", ""))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "B85612", ""));
        suspensionMessageProcessor.processSuspensionEvent(sampleMessage);

        verify(mofUpdatedEventPublisher).sendMessage("{\"nhsNumber\":\"9692294951\",\"managingOrganisationOdsCode\":\"B85612\"}");
        verify(mofNotUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldNotProcessMessagesWhichAreNotInCorrectFormat() {
        String message = "invalid message";
        Assertions.assertThrows(Exception.class, () -> suspensionMessageProcessor.processSuspensionEvent(message));
    }
}
