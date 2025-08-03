package com.example;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerTest {

    @Test
    void testUnsupportedMethodReturns400() {
        Handler handler = new Handler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("PATCH");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, new DummyContext());

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Método no soportado"));
    }

    @Test
    void testGetAllReturns200() {
        Handler handler = new Handler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, new DummyContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().startsWith("[") || response.getBody().contains("[]"));
    }
}
