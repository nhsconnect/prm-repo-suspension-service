package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.config.ToggleConfig;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.InvalidPdsRequestException;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;
import uk.nhs.prm.repo.suspension.service.publishers.MessagePublisherBroker;

@Slf4j
@Service
public class ManagingOrganisationService {

    private final PdsService pdsService;
    private final MessagePublisherBroker messagePublisherBroker;
    private final String repoOdsCode;
    private final ToggleConfig toggleConfig;
    private String allowedOdsCodes;


    public ManagingOrganisationService(PdsService pdsService, MessagePublisherBroker messagePublisherBroker, @Value("${repo.ods.code}") String repoOdsCode, ToggleConfig toggleConfig, @Value("${safe_listed_ods_codes}") String allowedOdsCodes) {
        this.pdsService = pdsService;
        this.messagePublisherBroker = messagePublisherBroker;
        this.repoOdsCode = repoOdsCode;
        this.toggleConfig = toggleConfig;
        this.allowedOdsCodes = allowedOdsCodes;
    }

    void processMofUpdate(String suspensionMessage, SuspensionEvent suspensionEvent, PdsAdaptorSuspensionStatusResponse response) {
        try {
            if (toggleConfig.isCanUpdateManagingOrganisationToRepo() && odsCodeSafeListCheck(suspensionEvent)) {
                updateMofToRepo(response, suspensionEvent);
            } else {
                updateMofToPreviousGp(response, suspensionEvent);
            }
        } catch (InvalidPdsRequestException invalidPdsRequestException) {
            messagePublisherBroker.invalidMessage(suspensionMessage, suspensionEvent.getNemsMessageId());
            throw invalidPdsRequestException;
        }
    }

    private boolean odsCodeSafeListCheck(SuspensionEvent suspensionEvent) {
        log.info("Repo process only safe listed ODS code toggle is : " + toggleConfig.isRepoProcessOnlySafeListedOdsCodes());
        if (toggleConfig.isRepoProcessOnlySafeListedOdsCodes()){
            log.info("Allowed ODS codes are: " + allowedOdsCodes + " and the ODS code for the patient is: " + suspensionEvent.getPreviousOdsCode());
            return allowedOdsCodes != null && allowedOdsCodes.contains(suspensionEvent.getPreviousOdsCode());
        }
        return true;
    }


    private void updateMofToPreviousGp(PdsAdaptorSuspensionStatusResponse pdsResponse, SuspensionEvent suspensionEvent) {
        if (canUpdateManagingOrganisation(pdsResponse.getManagingOrganisation(), suspensionEvent.previousOdsCode())) {
            var updateMofResponse = pdsService.updateMof(pdsResponse.getNhsNumber(), suspensionEvent.previousOdsCode(), pdsResponse.getRecordETag());
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            var isSuperseded = nhsNumberIsSuperseded(suspensionEvent.nhsNumber(), pdsResponse.getNhsNumber());
            messagePublisherBroker.mofUpdatedMessage(suspensionEvent.nemsMessageId(), suspensionEvent.previousOdsCode(), isSuperseded);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            messagePublisherBroker.mofNotUpdatedMessage(suspensionEvent.nemsMessageId(), false);
        }
    }

    private void updateMofToRepo(PdsAdaptorSuspensionStatusResponse pdsResponse, SuspensionEvent suspensionEvent) {
        if (canUpdateManagingOrganisation(pdsResponse.getManagingOrganisation(), repoOdsCode)) {
            var updateResponse = pdsService.updateMof(pdsResponse.getNhsNumber(), repoOdsCode, pdsResponse.getRecordETag());
            log.info("Managing Organisation field Updated to REPO ODS Code " + repoOdsCode);
            messagePublisherBroker.repoIncomingMessage(updateResponse, suspensionEvent);
        } else {
            log.error("Managing Organisation field is already set to Repo ODS code");
            messagePublisherBroker.mofNotUpdatedMessage(suspensionEvent.nemsMessageId(), true);
        }

    }

    private boolean canUpdateManagingOrganisation(String currentManagingOrganisation, String newManagingOrganisation) {
        return (currentManagingOrganisation == null || !currentManagingOrganisation.equals(newManagingOrganisation));
    }

    private boolean nhsNumberIsSuperseded(String nemsEventNhsNumber, String pdsNhsNumber) {
        return !nemsEventNhsNumber.equals(pdsNhsNumber);
    }
}