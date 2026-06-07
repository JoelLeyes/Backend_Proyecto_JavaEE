package com.nexolab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Servicio de IA usando Google Gemini API (gratuita).
 * Requiere la variable de entorno GEMINI_API_KEY.
 * Obtener API key gratis en: https://aistudio.google.com/app/apikey
 */
public class AIChatService {

    // Modelo Gemini 2.0 Flash — rápido y gratuito
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final String GEMINI_KEY_ENV = "GEMINI_API_KEY";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String geminiApiKey = System.getenv(GEMINI_KEY_ENV);

    /**
     * Obtiene una respuesta del asistente IA basada en la consulta del usuario.
     */
    public String obtenerRespuestaIA(String consulta) {
        try {
            if (consulta == null || consulta.trim().isEmpty()) {
                return "Por favor, escribí una pregunta.";
            }

            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                System.err.println("[AIChatService] GEMINI_API_KEY no configurada.");
                return "El asistente IA no está configurado. El administrador debe configurar la variable de entorno GEMINI_API_KEY.";
            }

            return llamarGeminiAPI(consulta.trim());

        } catch (Exception e) {
            System.err.println("[AIChatService] Error al llamar Gemini: " + e.getMessage());
            e.printStackTrace();
            return "Lo siento, ocurrió un error al contactar el asistente. Por favor, intentá nuevamente.";
        }
    }

    /**
     * Realiza la llamada HTTP a la API de Google Gemini.
     */
    private String llamarGeminiAPI(String consulta) throws IOException {
        String apiUrl = GEMINI_API_URL + geminiApiKey.trim();
        URL url = new URL(apiUrl);

        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod("POST");
        conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conexion.setDoOutput(true);
        conexion.setConnectTimeout(15000);
        conexion.setReadTimeout(30000);

        // Construir JSON de la petición al formato de Gemini
        String systemPrompt = construirSystemPrompt();
        String promptCompleto = systemPrompt + "\n\nConsulta del usuario: " + consulta;

        String jsonBody = buildGeminiRequest(promptCompleto);

        // Enviar cuerpo del request
        try (OutputStream os = conexion.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = conexion.getResponseCode();
        System.out.println("[AIChatService] Gemini API status: " + statusCode);

        if (statusCode != 200) {
            String errorBody = leerStream(conexion.getErrorStream());
            System.err.println("[AIChatService] Gemini error body: " + errorBody);
            conexion.disconnect();
            return interpretarErrorGemini(statusCode, errorBody);
        }

        String respuestaJson = leerStream(conexion.getInputStream());
        conexion.disconnect();

        return extraerTextoGemini(respuestaJson);
    }

    /**
     * Construye el system prompt con contexto de la plataforma NexoLab.
     */
    private String construirSystemPrompt() {
        return "Sos un asistente virtual de NexoLab, una plataforma de mensajería instantánea para empresas. " +
               "Respondé siempre en español de manera amigable, clara y concisa.\n\n" +
               "Funcionalidades de la plataforma:\n" +
               "- Mensajes de texto en chats privados y grupales\n" +
               "- Envío de archivos adjuntos (PDF, Word, Excel, imágenes)\n" +
               "- Reacciones a mensajes con emojis\n" +
               "- Responder mensajes específicos (reply)\n" +
               "- Indicador de \"escribiendo...\"\n" +
               "- Búsqueda de mensajes dentro del chat\n" +
               "- Gestión de grupos: agregar/expulsar miembros, renombrar grupo\n" +
               "- Modo oscuro/claro\n\n" +
               "Si la pregunta es general o no está relacionada con la plataforma, respondé igualmente " +
               "de forma útil. Mantené las respuestas cortas (máximo 3-4 oraciones).";
    }

    /**
     * Construye el JSON de la petición para la API de Gemini.
     */
    private String buildGeminiRequest(String prompt) {
        // Escapar el prompt para JSON
        String escapado = escaparJSON(prompt);
        return "{"
             + "\"contents\":[{"
             +   "\"parts\":[{\"text\":\"" + escapado + "\"}]"
             + "}],"
             + "\"generationConfig\":{"
             +   "\"temperature\":0.7,"
             +   "\"maxOutputTokens\":512,"
             +   "\"topP\":0.9"
             + "}"
             + "}";
    }

    /**
     * Extrae el texto generado de la respuesta JSON de Gemini.
     */
    private String extraerTextoGemini(String respuestaJson) {
        try {
            JsonNode raiz = objectMapper.readTree(respuestaJson);

            // Estructura de Gemini: candidates[0].content.parts[0].text
            JsonNode candidates = raiz.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String texto = parts.get(0).path("text").asText("").trim();
                    if (!texto.isEmpty()) {
                        return texto;
                    }
                }
            }

            // Verificar si hay error embebido
            if (raiz.has("error")) {
                String errMsg = raiz.path("error").path("message").asText("Error desconocido");
                System.err.println("[AIChatService] Error en JSON de Gemini: " + errMsg);
                return "El asistente no pudo generar una respuesta. Intentá de nuevo.";
            }

            System.err.println("[AIChatService] Respuesta inesperada de Gemini: " + respuestaJson);
            return "No pude procesar la respuesta. Por favor, intentá de nuevo.";

        } catch (Exception e) {
            System.err.println("[AIChatService] Error al parsear respuesta de Gemini: " + e.getMessage());
            return "Error al procesar la respuesta del asistente.";
        }
    }

    /**
     * Interpreta errores HTTP de Gemini y devuelve un mensaje amigable.
     */
    private String interpretarErrorGemini(int statusCode, String errorBody) {
        if (statusCode == 400) {
            return "La consulta no pudo ser procesada. Por favor, reformulá tu pregunta.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "La API key de Gemini no es válida o no tiene permisos. Contactá al administrador.";
        }
        if (statusCode == 429) {
            return "Se superó el límite de consultas al asistente. Intentá en unos minutos.";
        }
        if (statusCode == 503 || statusCode == 502) {
            return "El servicio de IA está temporalmente no disponible. Intentá en unos instantes.";
        }
        return "El asistente no está disponible en este momento (código " + statusCode + "). Intentá más tarde.";
    }

    /**
     * Lee un InputStream (o ErrorStream) a String.
     */
    private String leerStream(java.io.InputStream stream) {
        if (stream == null) return "";
        try (Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Escapa caracteres especiales para insertar en un string JSON.
     */
    private String escaparJSON(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
