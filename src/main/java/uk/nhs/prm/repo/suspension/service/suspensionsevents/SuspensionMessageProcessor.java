package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationPublisherMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionMessageProcessor {
    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;
    private final MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    private final PdsService pdsService;

    private final ObjectMapper mapper = new ObjectMapper();

    private final SuspensionEventParser parser;

    public void processSuspensionEvent(String suspensionMessage) {

        SuspensionEvent suspensionEvent = parser.parse(suspensionMessage, this);
        PdsAdaptorSuspensionStatusResponse response = pdsService.isSuspended(suspensionEvent.nhsNumber());

        if (Boolean.TRUE.equals(response.getIsSuspended())){
            try {
                updateMof(response.getRecordETag(), response.getManagingOrganisation(), suspensionMessage, suspensionEvent);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void updateMof(String recordETag, String newManagingOrganisation, String suspensionMessage, SuspensionEvent suspensionEvent) throws JsonProcessingException {
        if (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode())) {
            PdsAdaptorSuspensionStatusResponse updateMofResponse = pdsService.updateMof(suspensionEvent.nhsNumber(), suspensionEvent.previousOdsCode(), recordETag);
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            publishMofUpdateMessage(suspensionEvent.nhsNumber(), updateMofResponse);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void publishMofUpdateMessage(String nhsNumber, PdsAdaptorSuspensionStatusResponse updateMofResponse) {
        ManagingOrganisationPublisherMessage managingOrganisationPublisherMessage = new ManagingOrganisationPublisherMessage(nhsNumber,
                updateMofResponse.getManagingOrganisation());
        try {
            mofUpdatedEventPublisher.sendMessage(mapper.writeValueAsString(managingOrganisationPublisherMessage));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

}
