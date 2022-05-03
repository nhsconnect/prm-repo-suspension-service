package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.config.ToggleConfig;
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

    @Value("${repo.ods.code}")
    private String repoOdsCode;

    private final ToggleConfig toggleConfig;

    void processMofUpdate(String suspensionMessage, SuspensionEvent suspensionEvent, PdsAdaptorSuspensionStatusResponse response) {
        try {
            if (toggleConfig.isCanUpdateManagingOrganisationToRepo()) {
                updateMofToRepo(response, suspensionEvent);
            } else {
                updateMofToPreviousGp(response, suspensionEvent);
            }
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            messagePublisherBroker.invalidFormattedMessage(suspensionMessage, new NonSensitiveDataMessage(suspensionEvent.nemsMessageId(),
                    "NO_ACTION:INVALID_SUSPENSION").toJsonString());
            throw invalidPdsRequestException;
        }
    }

    private void updateMofToPreviousGp(PdsAdaptorSuspensionStatusResponse pdsResponse, SuspensionEvent suspensionEvent) {
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

    private void updateMofToRepo(PdsAdaptorSuspensionStatusResponse pdsResponse, SuspensionEvent suspensionEvent) {
        var updateMofResponse = pdsService.updateMof(pdsResponse.getNhsNumber(), repoOdsCode, pdsResponse.getRecordETag());
    }

    private boolean canUpdateManagingOrganisation(String newManagingOrganisation, SuspensionEvent suspensionEvent) {
        return (newManagingOrganisation == null || !newManagingOrganisation.equals(suspensionEvent.previousOdsCode()));
    }

    private boolean nhsNumberIsSuperseded(String nemsEventNhsNumber, String pdsNhsNumber) {
        return !nemsEventNhsNumber.equals(pdsNhsNumber);
    }
}