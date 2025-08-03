package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.*;
import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.response.ResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserRepository repo = new UserRepository("Users",
            DynamoDbClient.builder().region(Region.US_EAST_1).build());

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = Optional.ofNullable(request.getHttpMethod()).orElse("").toUpperCase();
        String id = Optional.ofNullable(request.getPathParameters()).map(p -> p.get("Id")).orElse(null);
        String body = request.getBody();

        context.getLogger().log("Method: " + method + " | Path Id: " + id + " | Body: " + body + "\n");

        try {
            return switch (method) {
                case "GET"    -> id != null ? getById(id) : getAll();
                case "POST"   -> createUser(body);
                case "PUT"    -> updateUser(id, body);
                case "DELETE"-> deleteUser(id);
                default       -> ResponseFactory.error(400, "Método no soportado: " + method);
            };
        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage() + "\n");
            return ResponseFactory.error(500, "Error interno: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getAll() {
        return ResponseFactory.json(200, repo.findAll());
    }

    private APIGatewayProxyResponseEvent getById(String id) {
        if (id == null || id.isBlank()) return ResponseFactory.error(400, "Falta parámetro 'Id'");
        return repo.findById(id)
                .map(user -> ResponseFactory.json(200, user))
                .orElse(ResponseFactory.error(404, "Usuario no encontrado"));
    }

    private APIGatewayProxyResponseEvent createUser(String body) throws Exception {
        if (body == null || body.isBlank()) return ResponseFactory.error(400, "Body vacío");
        User user = mapper.readValue(body, User.class);
        if (user.getId() == null || user.getId().isBlank())
            user.setId(java.util.UUID.randomUUID().toString());
        repo.save(user);
        return ResponseFactory.json(201, user);
    }

    private APIGatewayProxyResponseEvent updateUser(String id, String body) throws Exception {
        if (id == null || id.isBlank()) return ResponseFactory.error(400, "Falta parámetro 'Id'");
        if (body == null || body.isBlank()) return ResponseFactory.error(400, "Body vacío");
        User user = mapper.readValue(body, User.class);
        user.setId(id);
        repo.update(user);
        return ResponseFactory.successMessage("Usuario actualizado");
    }

    private APIGatewayProxyResponseEvent deleteUser(String id) {
        if (id == null || id.isBlank()) return ResponseFactory.error(400, "Falta parámetro 'Id'");
        repo.delete(id);
        return ResponseFactory.successMessage("Usuario eliminado");
    }
}
