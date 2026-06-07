package com.nexolab.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AIChatService {
    // API gratuita de Hugging Face - Conversational Model
    private static final String HF_API_URL = "https://api-inference.huggingface.co/models/microsoft/DialoGPT-medium";
    private static final String HF_API_TOKEN_ENV = "HF_API_TOKEN";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String hfApiToken = System.getenv(HF_API_TOKEN_ENV);

    /**     * Obtiene respuesta del agente IA basada en una consulta del usuario     */
    public String obtenerRespuestaIA(String consulta) {
        try {
            if (consulta == null || consulta.trim().isEmpty()) {
                return "Por favor, escribe una pregunta.";
            }

            // Construir el prompt con contexto
            String promptConContexto = construirPrompt(consulta);

            // Llamar a Hugging Face API (sin API key requerida para este modelo)
            return llamarHFAPI(promptConContexto);
        } catch (Exception e) {
            System.err.println("Error al obtener respuesta de IA: " + e.getMessage());
            e.printStackTrace();
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

    /**     * Llamada a la API de Hugging Face     */
    private String llamarHFAPI(String inputs) throws IOException {
        URL url = new URL(HF_API_URL);

        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod("POST");
        conexion.setRequestProperty("Content-Type", "application/json");
        if (hfApiToken != null && !hfApiToken.isBlank()) {
            conexion.setRequestProperty("Authorization", "Bearer " + hfApiToken.trim());
        }
        conexion.setDoOutput(true);
        conexion.setConnectTimeout(20000);
        conexion.setReadTimeout(30000);

        // Construir JSON del request
        String jsonRequest = "{\"inputs\":\"" + escaparJSON(inputs) + "\",\"options\":{\"wait_for_model\":true}}";

        // Enviar request
        byte[] salida = jsonRequest.getBytes(StandardCharsets.UTF_8);
        conexion.getOutputStream().write(salida);
        conexion.getOutputStream().flush();

        // Leer respuesta
        int codigoRespuesta = conexion.getResponseCode();
        System.out.println("HF API Response Code: " + codigoRespuesta);

        if (codigoRespuesta != 200) {
            String errorMsg = leerErrorStream(conexion);
            System.err.println("Error HF API (" + codigoRespuesta + "): " + errorMsg);
            conexion.disconnect();
            if (codigoRespuesta == 401 || codigoRespuesta == 403) {
                return "El servicio de IA no está configurado correctamente. Configura HF_API_TOKEN en el backend.";
            }
            if (codigoRespuesta == 429) {
                return "El servicio de IA está ocupado por límite de uso. Intenta nuevamente en unos minutos.";
            }
            if (codigoRespuesta == 503) {
                return "El modelo de IA está iniciando. Por favor intenta en unos segundos.";
            }
            if (codigoRespuesta >= 500) {
                return "El servicio de IA no está disponible temporalmente. Intenta más tarde.";
            }
            return "No se pudo obtener respuesta de IA (" + codigoRespuesta + ").";
        }

        String respuesta = leerRespuesta(conexion);
        conexion.disconnect();

        // Extraer el texto de la respuesta JSON
        return extraerTextoRespuesta(respuesta);
    }

    /**     * Lee la respuesta de la conexión HTTP     */
    private String leerRespuesta(HttpURLConnection conexion) throws IOException {
        Scanner scanner = new Scanner(conexion.getInputStream(), StandardCharsets.UTF_8);
        StringBuilder respuesta = new StringBuilder();

        while (scanner.hasNextLine()) {
            respuesta.append(scanner.nextLine());
        }
        scanner.close();

        return respuesta.toString();
    }

    /**     * Lee el stream de error     */
    private String leerErrorStream(HttpURLConnection conexion) {
        try {
            if (conexion.getErrorStream() == null) {
                return "Error desconocido";
            }
            Scanner scanner = new Scanner(conexion.getErrorStream(), StandardCharsets.UTF_8);
            StringBuilder error = new StringBuilder();
            while (scanner.hasNextLine()) {
                error.append(scanner.nextLine());
            }
            scanner.close();
            return error.toString();
        } catch (Exception e) {
            return "Error desconocido";
        }
    }

    /**     * Extrae el texto de la respuesta JSON     */
    private String extraerTextoRespuesta(String respuestaJson) {
        try {
            JsonNode raiz = objectMapper.readTree(respuestaJson);

            if (raiz.isObject() && raiz.has("error")) {
                return "El servicio de IA devolvió un error: " + raiz.get("error").asText();
            }

            // Estructura de Hugging Face: [{ "generated_text": "..." }]
            if (raiz.isArray() && raiz.size() > 0) {
                JsonNode primerElemento = raiz.get(0);
                if (primerElemento.has("generated_text")) {
                    String textoCompleto = primerElemento.get("generated_text").asText();

                    // Limpiar el texto: remover el prompt original si está incluido
                    if (textoCompleto.contains("Usuario pregunta:")) {
                        String[] partes = textoCompleto.split("Usuario pregunta:\"");
                        if (partes.length > 1) {
                            textoCompleto = partes[1].replace("\"", "").trim();
                        }
                    }

                    return textoCompleto.isEmpty() ?
                            "No pude generar una respuesta. Por favor intenta con otra pregunta." :
                            textoCompleto;
                }
            }

            if (raiz.isObject()) {
                if (raiz.has("generated_text")) return raiz.get("generated_text").asText();
                if (raiz.has("answer")) return raiz.get("answer").asText();
            }

            return "No se pudo procesar la respuesta de la IA.";
        } catch (Exception e) {
            System.err.println("Error al extraer texto de respuesta: " + e.getMessage());
            return "Error al procesar la respuesta.";
        }
    }

    /**     * Escapa caracteres especiales para JSON     */
    private String escaparJSON(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
