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
    private static final String GEMINI_API_KEY_ENV = "GEMINI_API_KEY";
    private static final String GEMINI_MODEL = "gemini-2.0-flash";
    private static final String FALLBACK_MESSAGE = "Lo siento, no puedo procesar tu consulta en este momento. Por favor, intenta más tarde.";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String geminiApiKey = normalizeEnv(System.getenv(GEMINI_API_KEY_ENV));

    public String obtenerRespuestaIA(String consulta) {
        if (consulta == null || consulta.trim().isEmpty()) {
            return "Por favor, escribí una pregunta.";
        }

        String respuestaLocal = responderLocalmente(consulta);
        if (respuestaLocal != null) {
            return respuestaLocal;
        }

        try {
            return llamarGeminiAPI(construirPrompt(consulta.trim()));
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

    private String llamarGeminiAPI(String prompt) throws IOException {
        if (isBlank(geminiApiKey)) {
            return "El asistente no está configurado. Falta la clave GEMINI_API_KEY.";
        }

        URL url = URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=" + geminiApiKey).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);

        String requestBody = buildGeminiRequest(prompt);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = connection.getResponseCode();
        String responseBody = leerStream(statusCode == 200 ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();

        if (statusCode != 200) {
            LOGGER.log(Level.WARNING, "Error Gemini API ({0}): {1}", new Object[]{statusCode, responseBody});
            return interpretarErrorGemini(statusCode, responseBody);
        }

        return extraerTextoGemini(responseBody);
    }

    private String buildGeminiRequest(String prompt) {
        return "{"
                + "\"systemInstruction\":{\"parts\":[{\"text\":\"" + escaparJSON(construirSystemPrompt()) + "\"}]},"
                + "\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\"" + escaparJSON(prompt) + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.7,\"maxOutputTokens\":250}"
                + "}";
    }

    private String construirSystemPrompt() {
        return "Sos un asistente de ayuda para una plataforma de chat y comunicación empresarial. " +
                "Mantené respuestas útiles, breves y enfocadas en el producto.";
    }

    private String extraerTextoGemini(String respuestaJson) {
        try {
            JsonNode raiz = objectMapper.readTree(respuestaJson);
            JsonNode candidates = raiz.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode first = candidates.get(0);
                JsonNode content = first.path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String text = parts.get(0).path("text").asText("").trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }

            if (raiz.has("error")) {
                return "El servicio de IA devolvió un error: " + raiz.path("error").toString();
            }

            return "No se pudo procesar la respuesta de la IA.";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al extraer texto de respuesta", e);
            return FALLBACK_MESSAGE;
        }
    }

    private String interpretarErrorGemini(int statusCode, String errorBody) {
        if (statusCode == 400) {
            return "La consulta no pudo ser procesada. Por favor, reformulá tu pregunta.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "No se pudo autenticar con el servicio de IA. Verificá la configuración.";
        }
        if (statusCode == 429) {
            return "El servicio de IA está ocupado por límite de uso. Intenta nuevamente en unos minutos.";
        }
        if (statusCode == 502 || statusCode == 503) {
            return FALLBACK_MESSAGE;
        }
        return "No se pudo obtener respuesta del servicio de IA.";
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

    private static String normalizeEnv(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
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