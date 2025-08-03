package com.example.repository;

import com.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRepositoryTest {

    private static final String TABLE = "Users";

    @Mock
    private DynamoDbClient dynamoDbClient;

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userRepository = new UserRepository(TABLE, dynamoDbClient);
    }

    @Test
    void testFindAllReturnsUsers() {
        Map<String, AttributeValue> item = Map.of(
                "Id", AttributeValue.fromS("1"),
                "name", AttributeValue.fromS("John"),
                "email", AttributeValue.fromS("john@example.com")
        );

        ScanResponse scanResponse = ScanResponse.builder()
                .items(List.of(item))
                .build();

        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

        List<User> users = userRepository.findAll();

        assertEquals(1, users.size());
        assertEquals("1", users.getFirst().getId());
        assertEquals("John", users.getFirst().getName());
        assertEquals("john@example.com", users.getFirst().getEmail());
    }

    @Test
    void testFindByIdReturnsUser() {
        String userId = "123";
        Map<String, AttributeValue> item = Map.of(
                "Id", AttributeValue.fromS(userId),
                "name", AttributeValue.fromS("Alice"),
                "email", AttributeValue.fromS("alice@example.com")
        );

        GetItemResponse response = GetItemResponse.builder().item(item).build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        Optional<User> userOpt = userRepository.findById(userId);

        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals(userId, user.getId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@example.com", user.getEmail());
    }

    @Test
    void testFindByIdReturnsEmptyWhenNotFound() {
        GetItemResponse response = GetItemResponse.builder().item(Map.of()).build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        Optional<User> userOpt = userRepository.findById("non-existent");

        assertTrue(userOpt.isEmpty());
    }

    @Test
    void testSaveCallsPutItem() {
        User user = new User("321", "Bob", "bob@example.com");

        userRepository.save(user);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());

        PutItemRequest request = captor.getValue();
        assertEquals(TABLE, request.tableName());
        assertEquals("321", request.item().get("Id").s());
        assertEquals("Bob", request.item().get("name").s());
        assertEquals("bob@example.com", request.item().get("email").s());
    }

    @Test
    void testUpdateCallsUpdateItem() {
        User user = new User("999", "Charlie", "charlie@example.com");

        userRepository.update(user);

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDbClient).updateItem(captor.capture());

        UpdateItemRequest request = captor.getValue();
        assertEquals(TABLE, request.tableName());
        assertEquals("999", request.key().get("Id").s());
        assertEquals("Charlie", request.expressionAttributeValues().get(":name").s());
        assertEquals("charlie@example.com", request.expressionAttributeValues().get(":email").s());
    }

    @Test
    void testDeleteCallsDeleteItem() {
        String userId = "555";

        userRepository.delete(userId);

        ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(dynamoDbClient).deleteItem(captor.capture());

        DeleteItemRequest request = captor.getValue();
        assertEquals(TABLE, request.tableName());
        assertEquals("555", request.key().get("Id").s());
    }
}
