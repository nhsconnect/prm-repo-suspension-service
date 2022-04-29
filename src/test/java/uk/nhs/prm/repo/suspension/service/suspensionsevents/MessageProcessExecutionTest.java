package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.data.LastUpdatedEventService;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;
import uk.nhs.prm.repo.suspension.service.publishers.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class MessageProcessExecutionTest {

    private MessageProcessExecution messageProcessExecution;

    @Mock
    private MessagePublisherBroker messagePublisherBroker;

    @Mock
    private LastUpdatedEventService lastUpdatedEventService;

    @Mock
    private ConcurrentThreadLock concurrentThreadLock;

    private static final String nemsMessageId = "A6FBE8C3-9144-4DDD-BFFE-B49A96456B29";

    private static final String PREVIOUS_ODS_CODE = "PREVIOUS_ODS_CODE";

    @Mock
    private PdsService pdsService;

    private static final String NHS_NUMBER = "9692294951";

    @BeforeEach
    public void setUp() {
        var mofService = new ManagingOrganisationService(pdsService, messagePublisherBroker);
        messageProcessExecution = new MessageProcessExecution(messagePublisherBroker,
                pdsService, lastUpdatedEventService, mofService, new SuspensionEventParser(), concurrentThreadLock);
    }

    @Test
    void shouldUpdateMofForSyntheticPatientsWhenToggleIsOn() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        setField(messageProcessExecution, "processOnlySyntheticPatients", "true");
        setField(messageProcessExecution, "syntheticPatientPrefix", "969");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "", false);
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, PREVIOUS_ODS_CODE, "", false);

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, PREVIOUS_ODS_CODE, "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        messageProcessExecution.run(suspendedMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, PREVIOUS_ODS_CODE, false );
        verifyNoMoreInteractions(messagePublisherBroker);
        verifyLock(NHS_NUMBER);
    }

    @Test
    void shouldUpdateMofForNonSyntheticPatientsWhenToggleIsOff() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        setField(messageProcessExecution, "processOnlySyntheticPatients", "false");
        setField(messageProcessExecution, "syntheticPatientPrefix", "999");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "", false);
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, PREVIOUS_ODS_CODE, "", false);

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, PREVIOUS_ODS_CODE, "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        messageProcessExecution.run(suspendedMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, PREVIOUS_ODS_CODE, false );
        verifyNoMoreInteractions(messagePublisherBroker);
        verifyLock(NHS_NUMBER);
    }

    @Test
    void shouldUpdateMofIncludingNemsMessageId() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"environment\":\"local\"}";

        setField(messageProcessExecution, "processOnlySyntheticPatients", "false");
        setField(messageProcessExecution, "syntheticPatientPrefix", "999");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "", false);
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, PREVIOUS_ODS_CODE, "", false);

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, PREVIOUS_ODS_CODE, "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        messageProcessExecution.run(suspendedMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, PREVIOUS_ODS_CODE, false );
        verifyNoMoreInteractions(messagePublisherBroker);
        verifyLock(NHS_NUMBER);

    }

    @Test
    void shouldUpdateMofForSyntheticPatientsWhenToggleIsOff() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        setField(messageProcessExecution, "processOnlySyntheticPatients", "false");
        setField(messageProcessExecution, "syntheticPatientPrefix", "969");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "", false);
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, PREVIOUS_ODS_CODE, "", false);

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, PREVIOUS_ODS_CODE, "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        messageProcessExecution.run(suspendedMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, PREVIOUS_ODS_CODE, false );
        verifyNoMoreInteractions(messagePublisherBroker);
        verifyLock(NHS_NUMBER);
    }

    @Test
    void shouldNotUpdateMofForNonSyntheticPatientsWhenToggleIsOn() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        setField(messageProcessExecution, "processOnlySyntheticPatients", "true");
        setField(messageProcessExecution, "syntheticPatientPrefix", "929");

        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, "A1000", "", "", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);

        messageProcessExecution.run(suspendedMessage);

        var notSyntheticMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:NOT_SYNTHETIC");
        assertEquals("NO_ACTION:NOT_SYNTHETIC", notSyntheticMessage.getStatus());
        verify(pdsService).isSuspended(NHS_NUMBER);
        verify(messagePublisherBroker).notSyntheticMessage(nemsMessageId);
        verifyNoMoreInteractions(pdsService);
        verifyNoMoreInteractions(messagePublisherBroker);
        verifyLock(NHS_NUMBER);
    }

    @Test
    void shouldPublishASuspensionMessageToNotSuspendedSNSTopicWhenPatientIsNotCurrentlySuspended() {
        var notSuspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, false, "null", "", "", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);

        messageProcessExecution.run(notSuspendedMessage);

        verify(messagePublisherBroker).notSuspendedMessage(nemsMessageId);
        verifyNoMoreInteractions(messagePublisherBroker);
        verifyLock(NHS_NUMBER);
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspended() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"ORIGINAL_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, "12345", "NEW_ODS_CODE", "", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "ORIGINAL_ODS_CODE", "")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        messageProcessExecution.run(suspendedMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, "ORIGINAL_ODS_CODE", false );
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspendedAndSuperseded() {
        final var ORIGINAL_NHS_NUMBER = NHS_NUMBER;
        final var SUPERSEDED_NHS_NUMBER = "1234567890";

        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"ORIGINAL_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"" + ORIGINAL_NHS_NUMBER + "\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        var pdsAdaptorSuspensionStatusResponse = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, "12345", "NEW_ODS_CODE", "ORIGINAL_E_TAG", false);
        var pdsAdaptorSuspensionStatusResponseSuperseded = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, null, "NEW_ODS_CODE", "SUPERSEDED_E_TAG", false);
        when(pdsService.isSuspended(ORIGINAL_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.isSuspended(SUPERSEDED_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponseSuperseded);
        when(pdsService.updateMof(SUPERSEDED_NHS_NUMBER, "ORIGINAL_ODS_CODE", "SUPERSEDED_E_TAG")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        messageProcessExecution.run(suspendedMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, "ORIGINAL_ODS_CODE", true );
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldUpdateMofWhenPatientIsConfirmedSuspended() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, "12345", "", "W/\"5\"", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "B85612", "W/\"5\"")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        messageProcessExecution.run(suspendedMessage);
        verify(pdsService).updateMof(NHS_NUMBER, "B85612", "W/\"5\"");
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientConfirmedSuspendedWhenPdsAlreadyHasADifferentMOFValue() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"LAST_GP_BEFORE_SUSPENSION_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        var pdsAdaptorLookUpSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "EXISTING_MOF_ODS_CODE", "", false);

        var pdsAdaptorUpdateSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "", false);

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorLookUpSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "")).thenReturn(pdsAdaptorUpdateSuspensionStatusResponse);

        messageProcessExecution.run(suspendedMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", false );
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldPublishSuspendedMessageToMofNotUpdatedSnsTopicWhenPatientMofAlreadySetToCorrectValue() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "B85612", "", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        messageProcessExecution.run(sampleMessage);

        verify(messagePublisherBroker).mofNotUpdatedMessage(nemsMessageId);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldParsePdsResponseWhenMofFieldNull() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, false, "B86041", null, "", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        messageProcessExecution.run(sampleMessage);

        verify(messagePublisherBroker).notSuspendedMessage(nemsMessageId);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldParsePdsResponseWhenCurrentOdsCodeFieldNull() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "B85612", "", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        messageProcessExecution.run(sampleMessage);

        verify(messagePublisherBroker).mofNotUpdatedMessage(nemsMessageId);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldParseSuspendedPdsResponseWhenMofFieldNull() {
        var sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "", false);
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "B85612", ""))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "B85612", "", false));
        messageProcessExecution.run(sampleMessage);

        verify(messagePublisherBroker).mofUpdatedMessage(nemsMessageId, "B85612", false );
        verifyNoMoreInteractions(messagePublisherBroker);
    }


    @Test
    void shouldLandOnDlqWhenSuspensionInvalid() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";
        when(pdsService.isSuspended(NHS_NUMBER)).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                messageProcessExecution.run(sampleMessage));

        String sampleNonSensitiveMessage = "{\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"status\":\"NO_ACTION:INVALID_SUSPENSION\"}";

        verify(messagePublisherBroker).invalidFormattedMessage(sampleMessage, sampleNonSensitiveMessage);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldPutInvalidSuspensionMessageOnDLQWhenMOFUpdateIfSuspensionInvalid() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";
        when(pdsService.isSuspended(NHS_NUMBER))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "", false));
        when(pdsService.updateMof(any(), any(), any())).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                messageProcessExecution.run(sampleMessage));

        String sampleNonSensitiveMessage = "{\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"status\":\"NO_ACTION:INVALID_SUSPENSION\"}";

        verify(messagePublisherBroker).invalidFormattedMessage(sampleMessage, sampleNonSensitiveMessage);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldPutNonSensitiveInvalidSuspensionMessageOnDLQWhenMOFUpdateIfSuspensionInvalid() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "", false));
        when(pdsService.updateMof(any(), any(), any())).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                messageProcessExecution.run(sampleMessage));

        String sampleNonSensitiveMessage = "{\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"status\":\"NO_ACTION:INVALID_SUSPENSION\"}";

        verify(messagePublisherBroker).invalidFormattedMessage(sampleMessage, sampleNonSensitiveMessage);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    @Test
    void shouldPutDeceasedPatientOnDeceasedPatientSnsTopic() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, null, null, null, "W/6", true));

        messageProcessExecution.run(sampleMessage);

        verify(messagePublisherBroker).deceasedPatientMessage(nemsMessageId);
        verifyNoMoreInteractions(messagePublisherBroker);
    }

    private void verifyLock(String nhsNumber) {
        verify(concurrentThreadLock).lock(nhsNumber);
        verify(concurrentThreadLock).unlock(nhsNumber);
    }
} 
