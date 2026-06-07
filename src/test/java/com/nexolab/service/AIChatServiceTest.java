package com.nexolab.service;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class AIChatServiceTest {

    @Test
    public void testGeminiApiDirectly() throws Exception {
        String apiKey = null;
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("GEMINI_API_KEY=")) {
                    apiKey = line.substring("GEMINI_API_KEY=".length()).trim();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer el archivo .env: " + e.getMessage());
        }

        if (apiKey == null) {
            System.out.println("GEMINI_API_KEY no encontrada en .env");
            return;
        }

        System.out.println("Usando API Key: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
        URL url = new URL(apiUrl);

        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod("POST");
        conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conexion.setDoOutput(true);

        String jsonBody = "{\"contents\":[{\"parts\":[{\"text\":\"Hola, respondé con un simple OK si recibís esto.\"}]}]}";

        try (OutputStream os = conexion.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = conexion.getResponseCode();
        System.out.println("HTTP Status Code de Gemini: " + statusCode);

        String responseBody;
        if (statusCode == 200) {
            responseBody = leerStream(conexion.getInputStream());
        } else {
            responseBody = leerStream(conexion.getErrorStream());
        }

        System.out.println("Respuesta de Gemini: " + responseBody);
        conexion.disconnect();
    }

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
}
