package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SuspensionsEventProcessorTest {

    @InjectMocks
    private SuspensionsEventProcessor suspensionsEventProcessor;

    @Mock
    private NotSuspendedEventPublisher notSuspendedEventPublisher;

    @Mock
    private MofUpdatedEventPublisher mofUpdatedEventPublisher;

    @Mock
    private MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;

    @Mock
    private PdsService pdsService;

    @Mock
    private SuspensionsEventParser suspensionsEventParser;


    @Test
    void shouldPublishASuspensionMessageToNotSuspendedSNSTopicWhenPatientIsNotCurrentlySuspended() throws JsonProcessingException {
        String notSuspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(false, "null", "", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("nhsNumber", "9692294951");
        returnMap.put("previousOdsCode", "B85612");
        when(suspensionsEventParser.mapMessageToHashMap(any())).thenReturn(returnMap);

        suspensionsEventProcessor.processSuspensionEvent(notSuspendedMessage);

        verify(notSuspendedEventPublisher).sendMessage(notSuspendedMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientIsConfirmedSuspended() throws JsonProcessingException {
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(true, "12345", "", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "B85612", "")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("nhsNumber", "9692294951");
        returnMap.put("previousOdsCode", "B85612");
        when(suspensionsEventParser.mapMessageToHashMap(any())).thenReturn(returnMap);
        when(suspensionsEventParser.parseMofUpdateMessage(any())).thenReturn(returnMap.toString());

        suspensionsEventProcessor.processSuspensionEvent(suspendedMessage);

        verify(mofUpdatedEventPublisher).sendMessage(returnMap.toString());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

    @Test
    void shouldUpdateMofWhenPatientIsConfirmedSuspended() throws JsonProcessingException {
        String suspendedMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";
        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(true, "12345", "", "W/\"5\"");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "B85612", "W/\"5\"")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("nhsNumber", "9692294951");
        returnMap.put("previousOdsCode", "B85612");
        when(suspensionsEventParser.mapMessageToHashMap(any())).thenReturn(returnMap);
        when(suspensionsEventParser.parseMofUpdateMessage(any())).thenReturn(returnMap.toString());

        suspensionsEventProcessor.processSuspensionEvent(suspendedMessage);
        verify(pdsService).updateMof("9692294951", "B85612", "W/\"5\"");
    }

    @Test
    void shouldPublishSuspendedMessageToMofUpdatedSnsTopicWhenPatientConfirmedSuspended() throws JsonProcessingException {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(true, null, "B85614", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);
        when(pdsService.updateMof("9692294951", "B85612", "")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        String messageJson = "{\"nhsNumber\":\"9692294951\",\"managingOrgainsationOdsCode\":\"B85612\"}";

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("nhsNumber", "9692294951");
        returnMap.put("previousOdsCode", "B85612");
        when(suspensionsEventParser.mapMessageToHashMap(any())).thenReturn(returnMap);
        when(suspensionsEventParser.parseMofUpdateMessage(any())).thenReturn(messageJson);

        suspensionsEventProcessor.processSuspensionEvent(sampleMessage);

        verify(mofUpdatedEventPublisher).sendMessage(messageJson);
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

    @Test
    void shouldPublishSuspendedMessageToMofNotUpdatedSnsTopicWhenPatientMofAlreadySetToCorrectValue() {
        String sampleMessage = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"9692294951\"}\",\"environment\":\"local\"}";

        PdsAdaptorSuspensionStatusResponse pdsAdaptorSuspensionStatusResponse
                = new PdsAdaptorSuspensionStatusResponse(true, null, "B85612", "");
        when(pdsService.isSuspended("9692294951")).thenReturn(pdsAdaptorSuspensionStatusResponse);

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("nhsNumber", "9692294951");
        returnMap.put("previousOdsCode", "B85612");
        when(suspensionsEventParser.mapMessageToHashMap(any())).thenReturn(returnMap);

        suspensionsEventProcessor.processSuspensionEvent(sampleMessage);

        verify(mofNotUpdatedEventPublisher).sendMessage(sampleMessage);
        verify(mofUpdatedEventPublisher, never()).sendMessage(any());
        verify(notSuspendedEventPublisher, never()).sendMessage(any());

    }

    @Test
    void shouldNotProcessMessagesWhichHasNotProperNhsNumber() {
        String message = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"nhsNumber\":\"invalid\"}\",\"environment\":\"local\"}";
        Assertions.assertThrows(Exception.class, () -> suspensionsEventProcessor.processSuspensionEvent(message));
    }

    @Test
    void shouldNotProcessMessagesWhichHaveNoNhsNumber() {
        String message = "{\"lastUpdated\":\"2017-11-01T15:00:33+00:00\",\"previousOdsCode\":\"B85612\",\"eventType\":\"SUSPENSION\",\"environment\":\"local\"}";
        Assertions.assertThrows(Exception.class, () -> suspensionsEventProcessor.processSuspensionEvent(message));
    }

    @Test
    void shouldNotProcessMessagesWhichAreNotInCorrectFormat() {
        String message = "invalid message";
        Assertions.assertThrows(Exception.class, () -> suspensionsEventProcessor.processSuspensionEvent(message));
    }
}
