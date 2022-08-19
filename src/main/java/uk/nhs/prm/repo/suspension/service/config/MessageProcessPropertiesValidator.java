package uk.nhs.prm.repo.suspension.service.config;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This component exists to validate the safe-listed NHS numbers
 */
@Component
public class MessageProcessPropertiesValidator {

    private final Pattern nhsPattern;
    private final Pattern odsCodePattern;

    public MessageProcessPropertiesValidator(MessageProcessProperties config) {
        this.odsCodePattern = Pattern.compile("\\w{6}");
        this.nhsPattern = Pattern.compile("\\d{10}");
        validate(config);
        validateOdsCodes(config);
    }

    public void validateOdsCodes(MessageProcessProperties messageProperties) {
        var odsCodes = messageProperties.getAllowedOdsCodes();
        if ("-".equals(odsCodes) || odsCodes == null) {
            return;
        }
        var odsCodesArray = odsCodes.split(",");
        for (var odsCode : odsCodesArray) {
            var match = odsCodePattern.matcher(odsCode);
            if (!match.matches()) {
                throw new RuntimeException("The provided ODS code in a safe list is invalid");
            }
        }
    }

    public void validate(MessageProcessProperties messageProperties) {
        var nhsNumbers = messageProperties.getAllowedPatientsNhsNumbers();
        if ("-".equals(nhsNumbers) || nhsNumbers == null) {
            return;
        }
        var nhsNumbersArray = nhsNumbers.split(",");

        for (var number : nhsNumbersArray) {
            var match = nhsPattern.matcher(number);
            if (!match.matches()) {
                throw new RuntimeException("The provided NHS number in a safe list is invalid");
            }
        }

    }
}