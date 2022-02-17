package uk.nhs.prm.repo.suspension.service.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.repo.suspension.service.config.DynamoDBClientSpringConfiguration;
import uk.nhs.prm.repo.suspension.service.metrics.AppConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DynamoDBClientSpringConfiguration.class})
public class DbClientTest {

    @Autowired
    private DynamoDBClientSpringConfiguration dynamoDBClientSpringConfiguration;

    @Disabled("WIP")
    @Test
    void shouldReadDataFromDbWithoutThrowing() {
        var dbClient = new DbClient(dynamoDBClientSpringConfiguration.dynamoDbClient(),
                new AppConfig("",
                        "",
                        "",
                        "",
                        "dev-suspension-service-dynamodb"));

        var nhsNumber = "123";
        Assertions.assertDoesNotThrow(() -> dbClient.getItem(nhsNumber));
    }
}
