package uk.nhs.prm.repo.suspension.service.config;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * This component exists to validate the safe-listed NHS numbers
 */
@Component
public class MessageProcessPropertiesValidator {

    private final Pattern nhsPattern;

    public MessageProcessPropertiesValidator(MessageProcessProperties config) {
        this.nhsPattern = Pattern.compile("\\d{10}");
        validate(config);
    }

    public void validate(MessageProcessProperties messageProperties) {
        var nhsNumbers = messageProperties.getAllowedPatientsNhsNumbers();
        if("-".equals(nhsNumbers) || nhsNumbers == null) {
            return;
        }
        var nhsNumbersArray = nhsNumbers.split(",");

        for(var number : nhsNumbersArray) {
            var match = nhsPattern.matcher(number);
            if(!match.matches()) {
                throw new RuntimeException("The provided NHS number in a safe list is invalid");
            }
        }

    }
}