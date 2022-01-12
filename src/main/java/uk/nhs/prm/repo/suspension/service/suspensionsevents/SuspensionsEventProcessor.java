package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.repo.suspension.service.model.ManagingOrganisationPublisherMessage;
import uk.nhs.prm.repo.suspension.service.model.PdsAdaptorSuspensionStatusResponse;
import uk.nhs.prm.repo.suspension.service.pds.PdsService;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuspensionsEventProcessor {
    private final NotSuspendedEventPublisher notSuspendedEventPublisher;
    private final MofUpdatedEventPublisher mofUpdatedEventPublisher;
    private final MofNotUpdatedEventPublisher mofNotUpdatedEventPublisher;
    private final PdsService pdsService;
    private final SuspensionsEventParser suspensionsEventParser;

    public void processSuspensionEvent(String suspensionMessage) {
        String nhsNumber = extractNhsNumber(suspensionMessage);
        PdsAdaptorSuspensionStatusResponse response = pdsService.isSuspended(nhsNumber);

        if (Boolean.TRUE.equals(response.getIsSuspended())) {
            try {
                updateMof(nhsNumber, response.getRecordETag(), response.getManagingOrganisation(), suspensionMessage);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else {
            notSuspendedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void updateMof(String nhsNumber, String recordETag, Object managingOrganisation, String suspensionMessage) throws JsonProcessingException {
        String previousOdsCode = extractPreviousOdsCode(suspensionMessage);
        if (!managingOrganisation.toString().equals(previousOdsCode)) {
            PdsAdaptorSuspensionStatusResponse updateMofResponse = pdsService.updateMof(nhsNumber, previousOdsCode, recordETag);
            log.info("Managing Organisation field Updated to " + updateMofResponse.getManagingOrganisation());
            publishMofUpdateMessage(nhsNumber, updateMofResponse);
        } else {
            log.info("Managing Organisation field is already set to previous GP");
            mofNotUpdatedEventPublisher.sendMessage(suspensionMessage);
        }
    }

    private void publishMofUpdateMessage(String nhsNumber, PdsAdaptorSuspensionStatusResponse updateMofResponse) {
        ManagingOrganisationPublisherMessage managingOrganisationPublisherMessage = new ManagingOrganisationPublisherMessage(nhsNumber,
                updateMofResponse.getManagingOrganisation().toString());
        try {
            mofUpdatedEventPublisher.sendMessage(suspensionsEventParser.parseMofUpdateMessage(managingOrganisationPublisherMessage));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    private String extractNhsNumber(String suspensionMessage) {
        Map<String, Object> map = suspensionsEventParser.mapMessageToHashMap(suspensionMessage);

        return validateNhsNumber(map);
    }

    private String extractPreviousOdsCode(String suspensionMessage) {
        Map<String, Object> map = suspensionsEventParser.mapMessageToHashMap(suspensionMessage);
        return validateOdsCode(map);
    }


    private String validateNhsNumber(Map<String, Object> map) {
        if (map.get("nhsNumber") == null) {
            log.error("Nhs number is not proper");
            throw new IllegalArgumentException("Message has no nhs number.");
        }
        String nhsNumber = map.get("nhsNumber").toString();
        if (!nhsNumber.matches("[0-9]+") && nhsNumber.length() != 10) {
            log.error("Nhs number is invalid.");
            throw new IllegalArgumentException("Nhs number is invalid.");
        }
        return nhsNumber;
    }

    private String validateOdsCode(Map<String, Object> map) {
        if (map.get("previousOdsCode") == null) {
            log.error("Ods can not be null");
            throw new IllegalArgumentException("Ods can not be null");
        }
        return map.get("previousOdsCode").toString();
    }
}
