package uk.nhs.prm.repo.suspension.service.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.repo.suspension.service.model.LastUpdatedData;
import uk.nhs.prm.repo.suspension.service.infra.LocalStackAwsConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest()
@ContextConfiguration(classes = { LocalStackAwsConfig.class})
public class DbClientTest {

    @Autowired
    DbClient dbClient;
    String nhsNumber = "1234567890";
    String lastUpdated = "2017-11-01T15:00:33+00:00";

    @BeforeEach
    public void setUp() {
        dbClient.addItem(new LastUpdatedData(nhsNumber, lastUpdated));
    }

    @Test
    void shouldReadDataFromSuspensionsDb() {
        var lastUpdatePatientData = dbClient.getItem(nhsNumber);
        assertThat(lastUpdatePatientData.getNhsNumber()).isEqualTo(nhsNumber);
        assertThat(lastUpdatePatientData.getLastUpdated()).isEqualTo(lastUpdated);
    }

    @Test
    void shouldHandleNhsNumberThatDoesNotExistInDb() {
        var notExistingNhsNumber = "9898989898";
        var lastUpdatePatientData = dbClient.getItem(notExistingNhsNumber);
        assertThat(lastUpdatePatientData).isEqualTo(null);
    }
}