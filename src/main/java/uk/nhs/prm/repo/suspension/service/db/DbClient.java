package uk.nhs.prm.repo.suspension.service.db;


import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class DbClient {
    private final DynamoDbClient dynamoDbClient;

    public void getItem() {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("nhs_number", AttributeValue.builder().s("123").build());
        GetItemResponse item = dynamoDbClient.getItem(GetItemRequest.builder().tableName("tableName").key(key).build());
        System.out.println(item);
    }
}
