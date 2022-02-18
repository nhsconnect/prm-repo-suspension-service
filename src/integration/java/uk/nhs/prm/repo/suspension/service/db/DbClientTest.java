package uk.nhs.prm.repo.suspension.service.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest()
@ContextConfiguration(classes = { LocalStackAwsDbConfig.class})
public class DbClientTest {

    @Autowired
    DbClient dbClient;

    @Disabled("To be fixed on CI")
    @Test
    void shouldReadDataFromDbWithoutThrowing() {
        var nhsNumber = "123";
        Assertions.assertDoesNotThrow(() -> dbClient.getItem(nhsNumber));
    }
}
