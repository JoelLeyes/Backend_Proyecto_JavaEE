package com.nexolab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.model.Chat;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Usuario;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PushNotificationService {
    private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/fcm/send";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String serverKey = System.getenv("FCM_SERVER_KEY");

    public void notifyMessage(Chat chat, Usuario sender, Mensaje message) {
        if (chat == null || sender == null || sender.getIdUsuario() == null) {
            return;
        }
        
        if (serverKey == null || serverKey.isBlank()) {
            System.err.println("FCM_SERVER_KEY no está configurado. Las notificaciones push no funcionarán.");
            return;
        }

        Set<String> tokens = new LinkedHashSet<>();
        if (chat.getParticipantes() != null) {
            for (Usuario participant : chat.getParticipantes()) {
                if (participant == null || participant.getIdUsuario() == null) {
                    continue;
                }
                if (participant.getIdUsuario().equals(sender.getIdUsuario())) {
                    continue;
                }
                String pushToken = participant.getPushToken();
                if (pushToken != null && !pushToken.isBlank()) {
                    tokens.add(pushToken.trim());
                }
            }
        }

        if (tokens.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("priority", "high");
        payload.put("notification", buildNotification(chat, sender, message));
        payload.put("data", buildData(chat, sender, message));
        if (tokens.size() == 1) {
            payload.put("to", tokens.iterator().next());
        } else {
            payload.put("registration_ids", new ArrayList<>(tokens));
        }

        send(payload, tokens.size());
    }

    private Map<String, Object> buildNotification(Chat chat, Usuario sender, Mensaje message) {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", buildTitle(chat, sender));
        notification.put("body", buildBody(message));
        notification.put("sound", "default");
        return notification;
    }

    private Map<String, Object> buildData(Chat chat, Usuario sender, Mensaje message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chatId", chat.getIdChat());
        data.put("chatName", chat.getNombreChat() == null ? "NexoLab" : chat.getNombreChat());
        data.put("senderId", sender.getIdUsuario());
        data.put("messageId", message == null ? null : message.getIdMensaje());
        data.put("unreadCount", "1");
        return data;
    }

    private String buildTitle(Chat chat, Usuario sender) {
        String senderName = ((sender.getNombre() == null ? "" : sender.getNombre()) + " " + (sender.getApellido() == null ? "" : sender.getApellido())).trim();
        if (senderName.isBlank()) {
            senderName = sender.getEmail() == null ? "NexoLab" : sender.getEmail();
        }
        String chatName = chat.getNombreChat() == null ? "chat" : chat.getNombreChat();
        return "Nuevo mensaje en " + chatName + " de " + senderName;
    }

    private String buildBody(Mensaje message) {
        if (message == null) {
            return "Tienes un nuevo mensaje.";
        }
        String content = message.getContenido();
        if (content == null || content.isBlank()) {
            return "Tienes un nuevo mensaje.";
        }
        content = content.trim();
        return content.length() > 140 ? content.substring(0, 140).trim() + "..." : content;
    }

    private void send(Map<String, Object> payload, int tokenCount) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(FCM_ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "key=" + serverKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            byte[] body = OBJECT_MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                System.out.println("Notificación FCM enviada exitosamente a " + tokenCount + " dispositivo(s).");
            } else {
                System.err.println("FCM respondió con estado " + status + " al enviar a " + tokenCount + " dispositivo(s).");
            }
        } catch (IOException e) {
            System.err.println("No se pudo enviar la notificación push: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}