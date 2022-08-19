package uk.nhs.prm.repo.suspension.service.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MessageProcessPropertiesValidatorTest {

    @Test
    public void shouldNotFailValidationWhenNhsNumbersAreValid() {
        var config = new MessageProcessProperties();
        config.setAllowedPatientsNhsNumbers("0123456789,0123456799");

        new MessageProcessPropertiesValidator(config);
    }

    @Test
    public void shouldNotFailValidationWhenConfigurationIsADash() {
        var config = new MessageProcessProperties();
        config.setAllowedPatientsNhsNumbers("-");

        new MessageProcessPropertiesValidator(config);
    }

    @Test
    public void shouldFailValidationWhenNhsNumbersAreNotValid() {
        var config = new MessageProcessProperties();
        config.setAllowedPatientsNhsNumbers("bogus");

        assertThrows(RuntimeException.class, () ->
                new MessageProcessPropertiesValidator(config));
    }

    @Test
    public void shouldNotFailValidationWhenOdsCodesAreValid() {
        var config = new MessageProcessProperties();
        config.setAllowedOdsCodes("odS123,456ODS");

        var messageProcessPropertiesValidator = new MessageProcessPropertiesValidator(config);
        messageProcessPropertiesValidator.validateOdsCodes(config);
    }
}
