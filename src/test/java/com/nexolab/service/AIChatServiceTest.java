package com.nexolab.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AIChatServiceTest {

    @Test
    void helloReturnsLocalGreetingWithoutCallingModel() {
        AIChatService service = new AIChatService();

        String response = service.obtenerRespuestaIA("hello");

        assertTrue(response.toLowerCase().contains("puedo ayudarte"));
    }

    @Test
    void ayudaReturnsLocalCapabilitiesSummary() {
        AIChatService service = new AIChatService();

        String response = service.obtenerRespuestaIA("ayuda");

        assertTrue(response.toLowerCase().contains("chat"));
    }
}
