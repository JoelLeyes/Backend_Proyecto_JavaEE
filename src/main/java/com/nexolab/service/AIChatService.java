package com.nexolab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AIChatService {
    private static final Logger LOGGER = Logger.getLogger(AIChatService.class.getName());
    private static final String OLLAMA_BASE_URL_ENV = "OLLAMA_BASE_URL";
    private static final String OLLAMA_MODEL_ENV = "OLLAMA_MODEL";
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://ollama:11434";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.2:1b";
    private static final String FALLBACK_MESSAGE = "El asistente local todavía no está listo. Si es la primera vez, el modelo se está descargando en el servidor.";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String ollamaBaseUrl = normalizeEnv(System.getenv(OLLAMA_BASE_URL_ENV), DEFAULT_OLLAMA_BASE_URL);
    private final String ollamaModel = normalizeEnv(System.getenv(OLLAMA_MODEL_ENV), DEFAULT_OLLAMA_MODEL);

    public String obtenerRespuestaIA(String consulta) {
        if (consulta == null || consulta.trim().isEmpty()) {
            return "Por favor, escribí una pregunta.";
        }

        String respuestaLocal = responderLocalmente(consulta);
        if (respuestaLocal != null) {
            return respuestaLocal;
        }

        try {
            return llamarOllamaAPI(construirPrompt(consulta.trim()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener respuesta de IA", e);
            return FALLBACK_MESSAGE;
        }
    }

    private String construirPrompt(String consulta) {
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
                "Si la pregunta es general o no está relacionada con la plataforma, respondé igualmente de forma útil. " +
                "Mantené las respuestas cortas (máximo 3-4 oraciones).\n\n" +
                "Consulta del usuario: " + consulta;
    }

    private String llamarOllamaAPI(String prompt) throws IOException {
        if (isBlank(ollamaBaseUrl) || isBlank(ollamaModel)) {
            return FALLBACK_MESSAGE;
        }

        URL url = URI.create(construirBaseUrl(ollamaBaseUrl) + "/api/chat").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);

        String requestBody = buildOllamaRequest(prompt);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = connection.getResponseCode();
        String responseBody = leerStream(statusCode == 200 ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();

        if (statusCode != 200) {
            LOGGER.log(Level.WARNING, "Error Ollama API ({0}): {1}", new Object[]{statusCode, responseBody});
            return interpretarErrorOllama(statusCode, responseBody);
        }

        return extraerTextoOllama(responseBody);
    }

    private String buildOllamaRequest(String prompt) {
        return "{"
                + "\"model\":\"" + escaparJSON(ollamaModel) + "\"," 
                + "\"stream\":false,"
                + "\"messages\":["
                +   "{\"role\":\"system\",\"content\":\"" + escaparJSON(construirSystemPrompt()) + "\"},"
                +   "{\"role\":\"user\",\"content\":\"" + escaparJSON(prompt) + "\"}"
                + "]"
                + "}";
    }

    private String construirSystemPrompt() {
        return "Sos un asistente de ayuda para una plataforma de chat y comunicación empresarial. " +
                "Mantené respuestas útiles, breves y enfocadas en el producto.";
    }

    private String extraerTextoOllama(String respuestaJson) {
        try {
            JsonNode raiz = objectMapper.readTree(respuestaJson);
            JsonNode message = raiz.path("message");
            String content = message.path("content").asText("").trim();
            if (!content.isEmpty()) {
                return content;
            }

            String response = raiz.path("response").asText("").trim();
            if (!response.isEmpty()) {
                return response;
            }

            if (raiz.has("error")) {
                return "El asistente local devolvió un error: " + raiz.path("error").asText();
            }

            return "No se pudo procesar la respuesta del asistente local.";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al extraer texto de respuesta", e);
            return FALLBACK_MESSAGE;
        }
    }

    private String interpretarErrorOllama(int statusCode, String errorBody) {
        if (statusCode == 400) {
            return "La consulta no pudo ser procesada. Por favor, reformulá tu pregunta.";
        }
        if (statusCode == 404) {
            return "El asistente local todavía no está disponible. Si es la primera vez, el modelo se está descargando en el servidor.";
        }
        if (statusCode >= 500) {
            return FALLBACK_MESSAGE;
        }
        return "No se pudo obtener respuesta del asistente local.";
    }

    private String responderLocalmente(String consulta) {
        String normalizada = normalizarConsulta(consulta);

        if (esSaludo(normalizada)) {
            return "Hola. Puedo ayudarte con el chat, archivos, reacciones, menciones, chats privados o grupos. Decime qué necesitás.";
        }

        if (normalizada.contains("que puedes hacer") || normalizada.contains("ayuda") || normalizada.contains("help")) {
            return "Puedo ayudarte con el chat, archivos, reacciones, menciones, chats privados, grupos y estado escribiendo. Preguntame algo concreto.";
        }

        return null;
    }

    private static boolean esSaludo(String consulta) {
        return consulta.equals("hola")
                || consulta.equals("hello")
                || consulta.equals("hi")
                || consulta.equals("buenas")
                || consulta.equals("buen dia")
                || consulta.equals("buenos dias")
                || consulta.equals("buenas tardes")
                || consulta.equals("buenas noches");
    }

    private static String normalizarConsulta(String consulta) {
        String limpia = Normalizer.normalize(consulta, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return limpia.toLowerCase(Locale.ROOT).trim();
    }

    private static String construirBaseUrl(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String normalizeEnv(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String leerStream(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try (Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
            }
            return builder.toString();
        }
    }

    private static String escaparJSON(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
