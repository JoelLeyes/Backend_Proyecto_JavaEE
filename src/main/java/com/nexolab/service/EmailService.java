package com.nexolab.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailService {

    public static class SmtpConfig {
        public final String host;
        public final int port;
        public final boolean startTls;
        public final String username;
        public final String password;
        public final String from;

        public SmtpConfig(String host, int port, boolean startTls, String username, String password, String from) {
            this.host = host;
            this.port = port;
            this.startTls = startTls;
            this.username = username;
            this.password = password;
            this.from = from;
        }
    }

    public SmtpConfig loadFromEnv() {
        String host = envOrNull("SMTP_HOST");
        String portStr = envOrNull("SMTP_PORT");
        String username = envOrNull("SMTP_USER");
        String password = envOrNull("SMTP_PASS");
        String from = envOrNull("SMTP_FROM");
        boolean startTls = Boolean.parseBoolean(envOrDefault("SMTP_STARTTLS", "true"));

        if (isBlank(host) || isBlank(portStr) || isBlank(username) || isBlank(password) || isBlank(from)) {
            return null;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return null;
        }

        return new SmtpConfig(host, port, startTls, username, password, from);
    }

    public void sendTextEmail(SmtpConfig cfg, String to, String subject, String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", cfg.host);
        props.put("mail.smtp.port", String.valueOf(cfg.port));
        props.put("mail.smtp.starttls.enable", String.valueOf(cfg.startTls));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.username, cfg.password);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(cfg.from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());
        msg.setText(body, StandardCharsets.UTF_8.name());

        Transport.send(msg);
    }

    private static String envOrNull(String key) {
        return System.getenv(key);
    }

    private static String envOrDefault(String key, String def) {
        String v = System.getenv(key);
        return v == null ? def : v;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
