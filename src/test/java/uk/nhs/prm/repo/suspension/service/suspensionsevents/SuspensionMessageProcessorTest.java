package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.db.EventOutOfDateService;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationUpdatedMessage;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.IntermittentErrorPdsException;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class SuspensionMessageProcessorTest {

    private SuspensionMessageProcessor suspensionMessageProcessor;

    private MessageProcessExecution messageProcessExecution;

    @Mock
    private NotSuspendedEventPublisher notSuspendedEventPublisher;

    @Mock
    private MofUpdatedEventPublisher mofUpdatedEventPublisher;

    @Mock
    private MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;

    @Mock
    private EventOutOfDateService eventOutOfDateService;

    @Mock
    private EventOutOfDatePublisher eventOutOfDatePublisher;

    @Mock
    private InvalidSuspensionPublisher invalidSuspensionPublisher;

    @Mock
    private ConcurrentThreadLock concurrentThreadLock;

    private static final String nemsMessageId = "A6FBE8C3-9144-4DDD-BFFE-B49A96456B29";

    @Mock
    private PdsService pdsService;

    private static final String NHS_NUMBER = "9692294951";

    @BeforeEach
    public void setUp() {
        messageProcessExecution = new MessageProcessExecution(notSuspendedEventPublisher, mofUpdatedEventPublisher,
                mofNotUpdatedEventPublisher, invalidSuspensionPublisher, eventOutOfDatePublisher,
                pdsService, eventOutOfDateService, new SuspensionEventParser(), concurrentThreadLock);
        suspensionMessageProcessor = new SuspensionMessageProcessor(messageProcessExecution);
        setField(suspensionMessageProcessor, "initialIntervalMillis", 1);
        setField(suspensionMessageProcessor, "maxAttempts", 5);
        setField(suspensionMessageProcessor, "multiplier", 2.0);
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "PREVIOUS_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION");

        suspensionMessageProcessor.process(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "PREVIOUS_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION");

        suspensionMessageProcessor.process(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "PREVIOUS_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION");

        suspensionMessageProcessor.process(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "PREVIOUS_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION");

        suspensionMessageProcessor.process(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionMessageProcessor.process(suspendedMessage);

        var notSyntheticMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:NOT_SYNTHETIC");
        assertEquals("NO_ACTION:NOT_SYNTHETIC", notSyntheticMessage.getStatus());
        verify(mofNotUpdatedEventPublisher).sendMessage(notSyntheticMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, false, "null", "", "");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionMessageProcessor.process(notSuspendedMessage);

        var expectedMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");
        verify(notSuspendedEventPublisher).sendMessage(expectedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, "12345", "NEW_ODS_CODE", "");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "ORIGINAL_ODS_CODE", "")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "ORIGINAL_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION");
        suspensionMessageProcessor.process(suspendedMessage);
        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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

        var pdsAdaptorSuspensionStatusResponse = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, "12345", "NEW_ODS_CODE", "ORIGINAL_E_TAG");
        var pdsAdaptorSuspensionStatusResponseSuperseded = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, null, "NEW_ODS_CODE", "SUPERSEDED_E_TAG");
        when(pdsService.isSuspended(ORIGINAL_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.isSuspended(SUPERSEDED_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponseSuperseded);
        when(pdsService.updateMof(SUPERSEDED_NHS_NUMBER, "ORIGINAL_ODS_CODE", "SUPERSEDED_E_TAG")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "ORIGINAL_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION_FOR_SUPERSEDED_PATIENT");
        suspensionMessageProcessor.process(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, "12345", "", "W/\"5\"");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "B85612", "W/\"5\"")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.process(suspendedMessage);
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "EXISTING_MOF_ODS_CODE", "");

        var pdsAdaptorUpdateSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "");

        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorLookUpSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "")).thenReturn(pdsAdaptorUpdateSuspensionStatusResponse);

        suspensionMessageProcessor.process(suspendedMessage);
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "ACTION:UPDATED_MANAGING_ORGANISATION");
        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "B85612", "");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.process(sampleMessage);
        var nonSensitiveDataMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:MOF_SAME_AS_PREVIOUS_GP");
        assertEquals("NO_ACTION:MOF_SAME_AS_PREVIOUS_GP", nonSensitiveDataMessage.getStatus());
        verify(mofNotUpdatedEventPublisher).sendMessage(nonSensitiveDataMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, false, "B86041", null, "");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.process(sampleMessage);

        var expectedMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");
        verify(notSuspendedEventPublisher).sendMessage(expectedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(mofNotUpdatedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "B85612", "");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.process(sampleMessage);
        var expectedMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:MOF_SAME_AS_PREVIOUS_GP");
        verify(mofNotUpdatedEventPublisher).sendMessage(expectedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
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
                = new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, "");
        when(pdsService.isSuspended(NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof(NHS_NUMBER, "B85612", ""))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, "B85612", ""));
        suspensionMessageProcessor.process(sampleMessage);
        var mofUpdatedMessage = new ManagingOrganisationUpdatedMessage(nemsMessageId, "B85612", "ACTION:UPDATED_MANAGING_ORGANISATION");

        verify(mofUpdatedEventPublisher).sendMessage(mofUpdatedMessage);
        verify(mofNotUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldRetryAsMaxAttemptNumber() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER)).thenThrow(IntermittentErrorPdsException.class);


        Assertions.assertThrows(IntermittentErrorPdsException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        int numberOfInvocations = 5;

        verify(pdsService, times(numberOfInvocations)).isSuspended(NHS_NUMBER);

    }

    @Test
    void shouldNotRetryIfNotGetException() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER)).thenThrow(IntermittentErrorPdsException.class)
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, false, null, null, ""));

        suspensionMessageProcessor.process(sampleMessage);

        int numberOfInvocations = 2;

        verify(pdsService, times(numberOfInvocations)).isSuspended(NHS_NUMBER);

    }

    @Test
    void shouldNotRetryWhenGotInvalidPdsRequestException() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended(NHS_NUMBER)).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        verify(pdsService, times(1)).isSuspended(NHS_NUMBER);

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
                suspensionMessageProcessor.process(sampleMessage));

        String sampleNonSensitiveMessage = "{\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"status\":\"NO_ACTION:INVALID_SUSPENSION\"}";

        verify(invalidSuspensionPublisher).sendMessage(sampleMessage);
        verify(invalidSuspensionPublisher).sendNonSensitiveMessage(sampleNonSensitiveMessage);
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
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, ""));
        when(pdsService.updateMof(any(), any(), any())).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        verify(invalidSuspensionPublisher).sendMessage(sampleMessage);
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
                .thenReturn(new PdsAdaptorSuspensionStatusResponse(NHS_NUMBER, true, null, null, ""));
        when(pdsService.updateMof(any(), any(), any())).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        String sampleNonSensitiveMessage = "{\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"status\":\"NO_ACTION:INVALID_SUSPENSION\"}";

        verify(invalidSuspensionPublisher).sendMessage(sampleMessage);
        verify(invalidSuspensionPublisher).sendNonSensitiveMessage(sampleNonSensitiveMessage);
    }

    @Test
    void shouldPutInvalidBogusMessageOnInvalidSuspensionMessageOnDLQ() {
        String sampleMessage = "invalid-bogus";

        Assertions.assertThrows(InvalidSuspensionMessageException.class, () ->
                suspensionMessageProcessor.process(sampleMessage));

        verify(invalidSuspensionPublisher).sendMessage(sampleMessage);
        verify(invalidSuspensionPublisher).sendNonSensitiveMessage(sampleMessage);
    }


    @Test
    void shouldNotProcessMessagesWhichAreNotInCorrectFormat() {
        String message = "invalid message";
        Assertions.assertThrows(Exception.class, () -> suspensionMessageProcessor.process(message));
    }

    private void verifyLock(String nhsNumber) {
        verify(concurrentThreadLock).lock(nhsNumber);
        verify(concurrentThreadLock).unlock(nhsNumber);
    }
} 
