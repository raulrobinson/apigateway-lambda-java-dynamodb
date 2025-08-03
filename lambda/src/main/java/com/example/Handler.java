package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "Users";
    private final DynamoDbClient dynamoDb = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = Optional.ofNullable(request.getHttpMethod()).orElse("").toUpperCase();
        String pathId = Optional.ofNullable(request.getPathParameters()).map(p -> p.get("Id")).orElse(null);
        String body = request.getBody();

        context.getLogger().log("Method: " + method + "\n");
        context.getLogger().log("Path Param Id: " + pathId + "\n");
        context.getLogger().log("Body: " + body + "\n");

        try {
            return switch (method) {
                case "GET"    -> pathId != null ? getById(pathId) : getAll();
                case "POST"   -> createUser(body);
                case "PUT"    -> updateUser(pathId, body);
                case "DELETE"-> deleteUser(pathId);
                default       -> response(400, "Método no soportado: " + method);
            };
        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage() + "\n");
            return response(500, "Error interno: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getAll() {
        try {
            List<Map<String, String>> users = dynamoDb.scan(ScanRequest.builder().tableName(TABLE).build())
                    .items()
                    .stream()
                    .map(item -> item.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> Optional.ofNullable(e.getValue().s()).orElse("")
                            )))
                    .collect(Collectors.toList());

            return response(200, users);
        } catch (Exception e) {
            return response(500, "Error al obtener usuarios: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getById(String id) {
        if (id == null || id.isBlank()) return response(400, "Falta parámetro 'Id'");

        try {
            var item = dynamoDb.getItem(GetItemRequest.builder()
                    .tableName(TABLE)
                    .key(Map.of("Id", AttributeValue.fromS(id)))
                    .build()).item();

            if (item == null || item.isEmpty())
                return response(404, Map.of("message", "Usuario no encontrado"));

            Map<String, String> userMap = item.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s()));

            return response(200, mapper.convertValue(userMap, User.class));
        } catch (Exception e) {
            return response(500, "Error al obtener usuario: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent createUser(String body) {
        if (body == null || body.isBlank()) return response(400, "Body vacío");

        try {
            User user = mapper.readValue(body, User.class);
            if (user.getId() == null || user.getId().isBlank())
                user.setId(UUID.randomUUID().toString());

            Map<String, AttributeValue> item = Map.of(
                    "Id", AttributeValue.fromS(user.getId()),
                    "name", AttributeValue.fromS(user.getName()),
                    "email", AttributeValue.fromS(user.getEmail())
            );

            dynamoDb.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
            return response(201, user);
        } catch (Exception e) {
            return response(500, "Error al crear usuario: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent updateUser(String id, String body) {
        if (id == null || id.isBlank()) return response(400, "Falta parámetro 'Id'");
        if (body == null || body.isBlank()) return response(400, "Body vacío");

        try {
            User user = mapper.readValue(body, User.class);

            Map<String, AttributeValue> key = Map.of("Id", AttributeValue.fromS(id));
            Map<String, String> attrNames = Map.of("#name", "name");
            Map<String, AttributeValue> attrValues = Map.of(
                    ":name", AttributeValue.fromS(user.getName()),
                    ":email", AttributeValue.fromS(user.getEmail())
            );

            dynamoDb.updateItem(UpdateItemRequest.builder()
                    .tableName(TABLE)
                    .key(key)
                    .updateExpression("SET #name = :name, email = :email")
                    .expressionAttributeNames(attrNames)
                    .expressionAttributeValues(attrValues)
                    .build());

            return response(200, Map.of("message", "Usuario actualizado"));
        } catch (Exception e) {
            return response(500, "Error al actualizar usuario: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent deleteUser(String id) {
        if (id == null || id.isBlank()) return response(400, "Falta parámetro 'Id'");

        try {
            dynamoDb.deleteItem(DeleteItemRequest.builder()
                    .tableName(TABLE)
                    .key(Map.of("Id", AttributeValue.fromS(id)))
                    .build());

            return response(200, Map.of("message", "Usuario eliminado"));
        } catch (Exception e) {
            return response(500, "Error al eliminar usuario: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent response(int statusCode, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(mapper.writeValueAsString(body));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Error de serialización\"}");
        }
    }
}
