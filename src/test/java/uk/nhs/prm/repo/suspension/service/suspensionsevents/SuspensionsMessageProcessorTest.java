package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.IntermittentErrorPdsException;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
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
    private InvalidSuspensionPublisher invalidSuspensionPublisher;

    @Mock
    private PdsService pdsService;

    @BeforeEach
    public void setUp() {
        suspensionMessageProcessor = new SuspensionMessageProcessor(notSuspendedEventPublisher, mofUpdatedEventPublisher,
                mofNotUpdatedEventPublisher, invalidSuspensionPublisher, pdsService, new SuspensionEventParser());
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
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "true");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "969");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        var messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldUpdateMofForNonSyntheticPatientsWhenToggleIsOff() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "false");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "999");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        var messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldUpdateMofIncludingNemsMessageId() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "false");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "999");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        var messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldUpdateMofForSyntheticPatientsWhenToggleIsOff() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "false");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "969");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        var pdsAdaptorMofUpdatedResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "PREVIOUS_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "PREVIOUS_ODS_CODE", "")).thenReturn(pdsAdaptorMofUpdatedResponse);

        var messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldNotUpdateMofForNonSyntheticPatientsWhenToggleIsOn() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"PREVIOUS_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        setField(suspensionMessageProcessor, "processOnlySyntheticPatients", "true");
        setField(suspensionMessageProcessor, "syntheticPatientPrefix", "929");
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofNotUpdatedEventPublisher).sendMessage(suspendedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishASuspensionMessageToNotSuspendedSNSTopicWhenPatientIsNotCurrentlySuspended() {
        String nemsMessageId = "A6FBE8C3-9144-4DDD-BFFE-B49A96456B29";
        var notSuspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", false, "null", "", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        suspensionMessageProcessor.processSuspensionEvent(notSuspendedMessage);

        var expectedMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS").toJsonString();
        verify(notSuspendedEventPublisher).sendMessage(expectedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspended() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"ORIGINAL_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, "12345", "NEW_ODS_CODE", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "ORIGINAL_ODS_CODE", "")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        var messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"NEW_ODS_CODE\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspendedAndSuperseded() {
        final var ORIGINAL_NHS_NUMBER = "9692294951";
        final var SUPERSEDED_NHS_NUMBER = "1234567890";

        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"ORIGINAL_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"" + ORIGINAL_NHS_NUMBER + "\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        var pdsAdaptorSuspensionStatusResponse = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, "12345", "NEW_ODS_CODE", "ORIGINAL_E_TAG");
        var pdsAdaptorSuspensionStatusResponseSuperseded = new PdsAdaptorSuspensionStatusResponse(SUPERSEDED_NHS_NUMBER, true, null, "NEW_ODS_CODE", "SUPERSEDED_E_TAG");
        when(pdsService.isSuspended(ORIGINAL_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.isSuspended(SUPERSEDED_NHS_NUMBER)).thenReturn(pdsAdaptorSuspensionStatusResponseSuperseded);
        when(pdsService.updateMof(SUPERSEDED_NHS_NUMBER, "ORIGINAL_ODS_CODE", "SUPERSEDED_E_TAG")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        var messageJson = "{\"nhsNumber\":\"" + SUPERSEDED_NHS_NUMBER + "\"," +
                "\"managingOrganisationOdsCode\":\"NEW_ODS_CODE\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}";

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldUpdateMofWhenPatientIsConfirmedSuspended() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";
        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, "12345", "", "W/\"5\"");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "B85612", "W/\"5\"")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);
        verify(pdsService).updateMof("9692294951", "B85612", "W/\"5\"");
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientConfirmedSuspendedWhenPdsAlreadyHasADifferentMOFValue() {
        var suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"LAST_GP_BEFORE_SUSPENSION_ODS_CODE\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        var pdsAdaptorLookUpSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "EXISTING_MOF_ODS_CODE", "");

        var pdsAdaptorUpdateSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "");

        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorLookUpSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "LAST_GP_BEFORE_SUSPENSION_ODS_CODE", "")).thenReturn(pdsAdaptorUpdateSuspensionStatusResponse);

        suspensionMessageProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage("{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"LAST_GP_BEFORE_SUSPENSION_ODS_CODE\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}");
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

    @Test
    void shouldPublishSuspendedMessageToMofNotUpdatedSnsTopicWhenPatientMofAlreadySetToCorrectValue() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
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
        String nemsMessageId = "A6FBE8C3-9144-4DDD-BFFE-B49A96456B29";
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", false, "B86041", null, "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        suspensionMessageProcessor.processSuspensionEvent(sampleMessage);

        var expectedMessage = new NonSensitiveDataMessage(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS").toJsonString();
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
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
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
        var sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        var pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "B85612", ""))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, "B85612", ""));
        suspensionMessageProcessor.processSuspensionEvent(sampleMessage);

        var messageJson = "{\"nhsNumber\":\"9692294951\"," +
                "\"managingOrganisationOdsCode\":\"B85612\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"}";

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(mofNotUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldRetryAsMaxAttemptNumber() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"}\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended("9692294951")).thenThrow(IntermittentErrorPdsException.class);


        Assertions.assertThrows(IntermittentErrorPdsException.class, () ->
                suspensionMessageProcessor.processSuspensionEvent(sampleMessage));

        int numberOfInvocations = 5;

        verify(pdsService, times(numberOfInvocations)).isSuspended("9692294951");

    }

    @Test
    void shouldNotRetryIfNotGetException() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended("9692294951")).thenThrow(IntermittentErrorPdsException.class)
                .thenReturn(new PdsAdaptorSuspensionStatusResponse("9692294951", false, null, null, ""));

        suspensionMessageProcessor.processSuspensionEvent(sampleMessage);

        int numberOfInvocations = 2;

        verify(pdsService, times(numberOfInvocations)).isSuspended("9692294951");

    }

    @Test
    void shouldNotRetryWhenGotInvalidPdsRequestException() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended("9692294951")).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.processSuspensionEvent(sampleMessage));

        verify(pdsService, times(1)).isSuspended("9692294951");

    }

    @Test
    void shouldLandOnDlqWhenSuspensionInvalid() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";
        when(pdsService.isSuspended("9692294951")).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.processSuspensionEvent(sampleMessage));

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
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";
        when(pdsService.isSuspended("9692294951"))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, ""));
        when(pdsService.updateMof(any(), any(), any())).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.processSuspensionEvent(sampleMessage));

        verify(invalidSuspensionPublisher).sendMessage(sampleMessage);
    }

    @Test
    void shouldPutNonSensitiveInvalidSuspensionMessageOnDLQWhenMOFUpdateIfSuspensionInvalid() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\"," +
                "\"previousOdsCode\":\"B85612\"," +
                "\"eventType\":\"SUSPENSION\"," +
                "\"nhsNumber\":\"9692294951\"," +
                "\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"environment\":\"local\"}";

        when(pdsService.isSuspended("9692294951"))
                .thenReturn(new PdsAdaptorSuspensionStatusResponse("9692294951", true, null, null, ""));
        when(pdsService.updateMof(any(), any(), any())).thenThrow(InvalidPdsRequestException.class);

        Assertions.assertThrows(InvalidPdsRequestException.class, () ->
                suspensionMessageProcessor.processSuspensionEvent(sampleMessage));

        String sampleNonSensitiveMessage = "{\"nemsMessageId\":\"A6FBE8C3-9144-4DDD-BFFE-B49A96456B29\"," +
                "\"status\":\"NO_ACTION:INVALID_SUSPENSION\"}";

        verify(invalidSuspensionPublisher).sendMessage(sampleMessage);
        verify(invalidSuspensionPublisher).sendNonSensitiveMessage(sampleNonSensitiveMessage);
    }


    @Test
    void shouldNotProcessMessagesWhichAreNotInCorrectFormat() {
        String message = "invalid message";
        Assertions.assertThrows(Exception.class, () -> suspensionMessageProcessor.processSuspensionEvent(message));
    }
}
