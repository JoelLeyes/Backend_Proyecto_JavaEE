package com.nexolab.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AIChatService {
    private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"; // Cambiar cuando tengas la clave
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtiene respuesta del agente IA basada en una consulta del usuario
     */
    public String obtenerRespuestaIA(String consulta) {
        try {
            // Construir el prompt con contexto de la plataforma
            String promptConContexto = construirPrompt(consulta);

            // Llamar a Gemini API
            return llamarGeminiAPI(promptConContexto);
        } catch (Exception e) {
            System.err.println("Error al obtener respuesta de IA: " + e.getMessage());
            return "Lo siento, no puedo procesar tu consulta en este momento. Por favor, intenta más tarde.";
        }
    }

    /**
     * Construye el prompt con contexto de la plataforma
     */
    private String construirPrompt(String consulta) {
        return "Eres un asistente de ayuda para una plataforma de chat y comunicación. " +
                "Debes responder preguntas frecuentes, dar recomendaciones de usuarios y contenido, " +
                "y proporcionar soporte técnico general sobre cómo funcionan las acciones en la página.\n\n" +
                "Información sobre la plataforma:\n" +
                "- Es una plataforma de mensajería instantánea para empresas\n" +
                "- Permite enviar mensajes de texto, archivos (PDF, DOC, XLS, imágenes)\n" +
                "- Permite crear chats privados y grupales\n" +
                "- Los usuarios pueden reaccionar a mensajes con emojis\n" +
                "- Soporta menciones de usuarios con @\n" +
                "- Tiene función de escribiendo...\n\n" +
                "Consulta del usuario: " + consulta + "\n\n" +
                "Proporciona una respuesta clara, concisa y útil. Si la pregunta no está relacionada " +
                "con la plataforma, sugiere que contacte al soporte técnico.";
    }

    /**
     * Llamada a la API de Gemini
     */
    private String llamarGeminiAPI(String prompt) throws IOException {
        // Construir URL con API key
        String urlConKey = GEMINI_API_URL + "?key=" + GEMINI_API_KEY;
        URL url = new URL(urlConKey);

        // Crear conexión
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod("POST");
        conexion.setRequestProperty("Content-Type", "application/json");
        conexion.setDoOutput(true);

        // Construir JSON del request
        String jsonRequest = construirJsonRequest(prompt);

        // Enviar request
        byte[] salida = jsonRequest.getBytes(StandardCharsets.UTF_8);
        conexion.getOutputStream().write(salida);
        conexion.getOutputStream().flush();

        // Leer respuesta
        int codigoRespuesta = conexion.getResponseCode();
        if (codigoRespuesta != 200) {
            throw new IOException("Gemini API error: " + codigoRespuesta);
        }

        String respuesta = leerRespuesta(conexion);
        conexion.disconnect();

        // Extraer el texto de la respuesta JSON
        return extraerTextoRespuesta(respuesta);
    }

    /**
     * Construye el JSON para el request de Gemini
     */
    private String construirJsonRequest(String prompt) throws IOException {
        try {
            return objectMapper.writeValueAsString(new Object() {
                public final Object[] contents = {
                        new Object() {
                            public final Object[] parts = {
                                    new Object() {
                                        public final String text = prompt;
                                    }
                            };
                        }
                };
            });
        } catch (Exception e) {
            throw new IOException("Error al construir JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Lee la respuesta de la conexión HTTP
     */
    private String leerRespuesta(HttpURLConnection conexion) throws IOException {
        Scanner scanner = new Scanner(conexion.getInputStream(), StandardCharsets.UTF_8);
        StringBuilder respuesta = new StringBuilder();

        while (scanner.hasNextLine()) {
            respuesta.append(scanner.nextLine());
        }
        scanner.close();

        return respuesta.toString();
    }

    /**
     * Extrae el texto de la respuesta JSON de Gemini
     */
    private String extraerTextoRespuesta(String respuestaJson) {
        try {
            JsonNode raiz = objectMapper.readTree(respuestaJson);

            // Navegar por la estructura: candidates[0].content.parts[0].text
            if (raiz.has("candidates") && raiz.get("candidates").isArray()) {
                JsonNode primerCandidato = raiz.get("candidates").get(0);
                if (primerCandidato.has("content")) {
                    JsonNode contenido = primerCandidato.get("content");
                    if (contenido.has("parts") && contenido.get("parts").isArray()) {
                        JsonNode primerParte = contenido.get("parts").get(0);
                        if (primerParte.has("text")) {
                            return primerParte.get("text").asText();
                        }
                    }
                }
            }

            return "No se pudo procesar la respuesta de la IA.";
        } catch (Exception e) {
            System.err.println("Error al extraer texto de respuesta: " + e.getMessage());
            return "Error al procesar la respuesta.";
        }
    }
}