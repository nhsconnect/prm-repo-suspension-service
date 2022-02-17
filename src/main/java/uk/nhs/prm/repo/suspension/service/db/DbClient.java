package uk.nhs.prm.repo.suspension.service.db;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import uk.nhs.prm.repo.suspension.service.metrics.AppConfig;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DbClient {
    private final DynamoDbClient dynamoDbClient;
    private final AppConfig config;

    //TODO: create custom object
    public Object getItem(String nhsNumber ) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("nhs_number", AttributeValue.builder().n(nhsNumber).build());
        return dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(config.suspensionDynamoDbTableName())
                .key(key)
                .build());
    }
}
