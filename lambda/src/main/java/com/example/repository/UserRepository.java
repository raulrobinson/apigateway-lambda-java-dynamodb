package com.example.repository;

import com.example.model.User;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class UserRepository {

    private final String tableName;
    private final DynamoDbClient dynamoDb;

    public UserRepository(String tableName, DynamoDbClient dynamoDb) {
        this.tableName = tableName;
        this.dynamoDb = dynamoDb;
    }

    public List<User> findAll() {
        var result = dynamoDb.scan(ScanRequest.builder().tableName(tableName).build());
        return result.items().stream().map(this::toUser).collect(Collectors.toList());
    }

    public Optional<User> findById(String id) {
        var response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("Id", AttributeValue.fromS(id)))
                .build());

        if (response.item() == null || response.item().isEmpty()) return Optional.empty();
        return Optional.of(toUser(response.item()));
    }

    public void save(User user) {
        Map<String, AttributeValue> item = Map.of(
                "Id", AttributeValue.fromS(user.getId()),
                "name", AttributeValue.fromS(user.getName()),
                "email", AttributeValue.fromS(user.getEmail())
        );

        dynamoDb.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
    }

    public void update(User user) {
        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("Id", AttributeValue.fromS(user.getId())))
                .updateExpression("SET #name = :name, email = :email")
                .expressionAttributeNames(Map.of("#name", "name"))
                .expressionAttributeValues(Map.of(
                        ":name", AttributeValue.fromS(user.getName()),
                        ":email", AttributeValue.fromS(user.getEmail())
                ))
                .build());
    }

    public void delete(String id) {
        dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("Id", AttributeValue.fromS(id)))
                .build());
    }

    private User toUser(Map<String, AttributeValue> item) {
        return User.builder()
                .id(item.getOrDefault("Id", AttributeValue.fromS("")).s())
                .name(item.getOrDefault("name", AttributeValue.fromS("")).s())
                .email(item.getOrDefault("email", AttributeValue.fromS("")).s())
                .build();
    }
}
