package com.nexolab.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.service.AIChatService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/ai/chat")
public class AIServlet extends HttpServlet {
    private final AIChatService aiChatService = new AIChatService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            // Verificar autenticación
            String token = req.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"No autorizado\"}");
                return;
            }

            // Parsear request JSON
            @SuppressWarnings("unchecked")
            Map<String, String> parametros = objectMapper.readValue(req.getInputStream(), Map.class);
            String consulta = parametros.get("consulta");

            if (consulta == null || consulta.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Consulta requerida\"}");
                return;
            }

            // Obtener respuesta de IA
            String respuestaIA = aiChatService.obtenerRespuestaIA(consulta);

            // Devolver respuesta
            Map<String, String> resultado = new HashMap<>();
            resultado.put("respuesta", respuestaIA);
            resultado.put("timestamp", String.valueOf(System.currentTimeMillis()));

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(objectMapper.writeValueAsString(resultado));

        } catch (Exception e) {
            System.err.println("Error en AIServlet: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error interno del servidor";
            resp.getWriter().write("{\"error\": \"" + errorMsg + "\"}");
        }
    }
}