package uk.nhs.prm.repo.suspension.service.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest()
@ContextConfiguration(classes = { LocalStackAwsDbConfig.class})
public class DbClientTest {

    @Autowired
    DbClient dbClient;

    @BeforeEach
    public void setUp() {
        dbClient.addItem("123", "123");
    }

    @Test
    void shouldReadDataFromDbWithoutThrowing() {
        var nhsNumber = "123";
        assertThat(dbClient.getItem(nhsNumber)).isInstanceOf(GetItemResponse.class);
    }
}
