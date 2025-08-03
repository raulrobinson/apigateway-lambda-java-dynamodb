package com.example.response;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ResponseFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static APIGatewayProxyResponseEvent json(int statusCode, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(mapper.writeValueAsString(body));
        } catch (Exception e) {
            return error(500, "Error de serialización");
        }
    }

    public static APIGatewayProxyResponseEvent error(int status, String message) {
        return json(status, Map.of("error", message));
    }

    public static APIGatewayProxyResponseEvent successMessage(String message) {
        return json(200, Map.of("message", message));
    }
}
