package uk.nhs.prm.repo.suspension.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SyntheticPatientConfig {

    @Bean
    public Void logNumberInSyntheticPatientList(@Value("${safe_listed_patients_nhs_numbers}") String safeList) {
        var numberOfNhsNumbersInSafeList = safeList.split(",").length;
        log.info("Safe list of nhs number has a size of {}", numberOfNhsNumbersInSafeList);
        return null;
    }

}
