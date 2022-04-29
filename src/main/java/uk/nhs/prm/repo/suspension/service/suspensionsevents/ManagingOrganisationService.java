package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.NonSensitiveDataMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;
import uk.nhs.prm.repo.suspension.service.publishers.MessagePublisherBroker;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagingOrganisationService {

    private final PdsService pdsService;
    private final MessagePublisherBroker messagePublisherBroker;


    void processMofUpdate(String suspensionMessage, SuspensionEvent suspensionEvent, PdsAdaptorSuspensionStatusResponse response) {
        try {
            updateMof(response, suspensionEvent);
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            messagePublisherBroker.invalidFormattedMessage(suspensionMessage, new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(),
                    "NO_ACTION:INVALID_SUSPENSION").toJsonString());
            throw invalidPdsRequestException;
        }
    }

    private void updateMof(PdsAdaptorSuspensionStatusResponse pdsResponse, SuspensionEvent suspensionEvent)  {
        if (canUpdateManagingOrganisation(pdsResponse.getManagingOrganisation(), suspensionEvent)) {
            var updateMofResponse = pdsService.updateMof(pdsResponse.getNhsNumber(), suspensionEvent.previousOdsCode(), pdsResponse.getRecordETag());
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            var isSuperseded = nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), pdsResponse.getNhsNumber());
            messagePublisherBroker.mofUpdatedMessage(suspensionEvent.nemsMessageId(), suspensionEvent.previousOdsCode(), isSuperseded);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            messagePublisherBroker.mofNotUpdatedMessage(suspensionEvent.nemsMessageId());
        }
    }

    private boolean canUpdateManagingOrganisation(String newManagingOrganisation, SuspensionEvent suspensionEvent) {
        return (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode()));
    }

    private boolean nhsNumberIsSuperseded(String nemsEventNhsNumber, String pdsNhsNumber) {
        return !nemsEventNhsNumber.equals(pdsNhsNumber);
    }
}