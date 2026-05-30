package com.nexolab.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.model.Chat;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Usuario;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PushNotificationService {

    private static final Logger log = Logger.getLogger(PushNotificationService.class.getName());
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String projectId;
    private final String clientEmail;
    private final PrivateKey privateKey;

    // Token cacheado — se renueva antes de los 60 min de expiración
    private volatile String cachedToken;
    private volatile long tokenExpiryMs = 0;

    public PushNotificationService() {
        String json = System.getenv("FCM_SERVICE_ACCOUNT_JSON");
        if (json == null || json.isBlank()) {
            log.warning("FCM_SERVICE_ACCOUNT_JSON no configurado. Notificaciones push deshabilitadas.");
            projectId = null; clientEmail = null; privateKey = null;
            return;
        }
        Map<String, Object> sa;
        try {
            sa = MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.severe("FCM_SERVICE_ACCOUNT_JSON no es JSON válido: " + e.getMessage());
            projectId = null; clientEmail = null; privateKey = null;
            return;
        }
        projectId   = (String) sa.get("project_id");
        clientEmail = (String) sa.get("client_email");
        privateKey  = loadPrivateKey((String) sa.get("private_key"));
        if (projectId == null || clientEmail == null || privateKey == null) {
            log.severe("FCM_SERVICE_ACCOUNT_JSON incompleto: faltan project_id, client_email o private_key.");
        }
    }

    private static PrivateKey loadPrivateKey(String pem) {
        if (pem == null) return null;
        try {
            String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----",   "")
                .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(stripped);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            log.severe("No se pudo cargar la private key FCM: " + e.getMessage());
            return null;
        }
    }

    public void notifyMessage(Chat chat, Usuario sender, Mensaje message) {
        if (projectId == null || clientEmail == null || privateKey == null) return;
        if (chat == null || sender == null || chat.getParticipantes() == null) return;

        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (Exception e) {
            log.warning("No se pudo obtener access token FCM: " + e.getMessage());
            return;
        }

        String fcmUrl = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

        for (Usuario participant : chat.getParticipantes()) {
            if (participant == null || participant.getIdUsuario() == null) continue;
            if (participant.getIdUsuario().equals(sender.getIdUsuario()))  continue;
            String token = participant.getPushToken();
            if (token == null || token.isBlank()) continue;

            try {
                sendToToken(fcmUrl, accessToken, token.trim(), chat, sender, message);
            } catch (Exception e) {
                log.warning("Error enviando notificación push: " + e.getMessage());
            }
        }
    }

    private void sendToToken(String fcmUrl, String accessToken, String deviceToken,
                             Chat chat, Usuario sender, Mensaje message) throws IOException {

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", "Nuevo mensaje de " + buildName(sender));
        notification.put("body",  buildBody(message));

        Map<String, String> data = new LinkedHashMap<>();
        data.put("chatId",    String.valueOf(chat.getIdChat()));
        data.put("chatName",  chat.getNombreChat() == null ? "NexoLab" : chat.getNombreChat());
        data.put("senderId",  String.valueOf(sender.getIdUsuario()));
        data.put("messageId", message == null ? "" : String.valueOf(message.getIdMensaje()));

        Map<String, Object> android = new LinkedHashMap<>();
        android.put("priority", "high");

        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("token",        deviceToken);
        msg.put("notification", notification);
        msg.put("data",         data);
        msg.put("android",      android);

        byte[] body = MAPPER.writeValueAsBytes(Map.of("message", msg));

        HttpURLConnection conn = (HttpURLConnection) new URL(fcmUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization",  "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type",   "application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) { os.write(body); }

        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            log.info("Notificación FCM enviada OK.");
        } else {
            String resp = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.warning("FCM respondió " + status + ": " + resp);
        }
        conn.disconnect();
    }

    // Obtiene (o reutiliza del caché) un OAuth2 access token para FCM v1.
    private synchronized String getAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiryMs) return cachedToken;

        long iat = now / 1000;
        long exp = iat + 3600;

        String header  = b64url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = b64url("{\"iss\":\"" + clientEmail + "\"," +
            "\"scope\":\"https://www.googleapis.com/auth/firebase.messaging\"," +
            "\"aud\":\"https://oauth2.googleapis.com/token\"," +
            "\"iat\":" + iat + ",\"exp\":" + exp + "}");

        String unsigned = header + "." + payload;
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(unsigned.getBytes(StandardCharsets.UTF_8));
        String jwt = unsigned + "." +
            Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());

        String form = "grant_type=" + enc("urn:ietf:params:oauth:grant-type:jwt-bearer")
                    + "&assertion=" + enc(jwt);

        HttpURLConnection conn = (HttpURLConnection) new URL(TOKEN_ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(form.getBytes(StandardCharsets.UTF_8));
        }

        String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();

        Map<String, Object> tokenResp = MAPPER.readValue(resp, new TypeReference<>() {});
        String accessToken = (String) tokenResp.get("access_token");
        if (accessToken == null) throw new Exception("access_token no presente en respuesta OAuth2");

        cachedToken   = accessToken;
        tokenExpiryMs = now + 55 * 60 * 1000L;   // renueva 5 min antes de expirar
        return cachedToken;
    }

    private static String b64url(String s) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String enc(String s) throws Exception {
        return java.net.URLEncoder.encode(s, "UTF-8");
    }

    private static String buildName(Usuario u) {
        String n = ((u.getNombre()   == null ? "" : u.getNombre())  + " "
                  + (u.getApellido() == null ? "" : u.getApellido())).trim();
        return n.isBlank() ? (u.getEmail() == null ? "NexoLab" : u.getEmail()) : n;
    }

    private static String buildBody(Mensaje m) {
        if (m == null) return "Tienes un nuevo mensaje.";
        String c = m.getContenido();
        if (c == null || c.isBlank()) return "Tienes un nuevo mensaje.";
        c = c.trim();
        return c.length() > 140 ? c.substring(0, 140) + "..." : c;
    }
}
