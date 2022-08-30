package uk.nhs.prm.repo.suspension.service.config;

import org.springframework.stereotype.Component;

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
        String[] nhsNumbersArray = getSplitValuesByComma(nhsNumbers);
        if (nhsNumbersArray == null) return;

        matchPatternForSafeList(nhsNumbersArray, nhsPattern, "The provided NHS number in a safe list is invalid");

    }

    private void matchPatternForSafeList(String[] safeListVariablesArray, Pattern pattern, String message) {
        for (var safeListVariable : safeListVariablesArray) {
            var match = pattern.matcher(safeListVariable);
            if (!match.matches()) {
                throw new RuntimeException(message);
            }
        }
    }

    private String[] getSplitValuesByComma(String safeListVariable) {
        if ("-".equals(safeListVariable) || safeListVariable == null) {
            return null;
        }
        var safeListVariablesArray = safeListVariable.split(",");
        return safeListVariablesArray;
    }
}