package uk.nhs.prm.repo.suspension.service.db;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import uk.nhs.prm.repo.suspension.service.metrics.AppConfig;
import uk.nhs.prm.repo.suspension.service.model.LastUpdatedData;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DbClient {
    private final DynamoDbClient dynamoDbClient;
    private final AppConfig config;

    public LastUpdatedData getItem(String nhsNumber ) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("nhs_number", AttributeValue.builder().n(nhsNumber).build());
        var getItemResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(config.suspensionDynamoDbTableName())
                .key(key)
                .build());

        return fromDbItem(getItemResponse);
    }

    public void addItem(LastUpdatedData lastUpdatedData) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("nhs_number", AttributeValue.builder().n(lastUpdatedData.getNhsNumber()).build());
        item.put("last_updated", AttributeValue.builder().n(lastUpdatedData.getLastUpdated()).build());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(config.suspensionDynamoDbTableName())
                .item(item)
                .build());
    }

    private LastUpdatedData fromDbItem(GetItemResponse itemResponse) {
        if (!itemResponse.hasItem()) {
            return null;
        }
        var nhsNumber = itemResponse.item().get("nhs_number").n();
        var lastUpdated = itemResponse.item().get("last_updated").n();
        return new LastUpdatedData(nhsNumber, lastUpdated);
    }
}
