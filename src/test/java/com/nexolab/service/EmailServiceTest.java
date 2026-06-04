package com.nexolab.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    @Test
    void loadFromEnvParsesSmtpConfig() {
        EmailService service = new EmailService();
        EmailService.SmtpConfig config = service.loadFromEnv(Map.of(
                "SMTP_HOST", "smtp.gmail.com",
                "SMTP_PORT", "587",
                "SMTP_USER", "user@gmail.com",
                "SMTP_PASS", "1234 5678 9012 3456",
                "SMTP_FROM", "user@gmail.com",
                "SMTP_STARTTLS", "true"
        ));

        assertTrue(config != null);
        assertEquals("smtp.gmail.com", config.host);
        assertEquals(587, config.port);
        assertTrue(config.startTls);
        assertEquals("user@gmail.com", config.username);
        assertEquals("1234567890123456", config.password);
        assertEquals("user@gmail.com", config.from);
    }

    @Test
    void loadFromEnvReturnsNullWhenRequiredValuesAreMissing() {
        EmailService service = new EmailService();

        assertNull(service.loadFromEnv(Map.of(
                "SMTP_HOST", "",
                "SMTP_PORT", "587",
                "SMTP_USER", "user@gmail.com",
                "SMTP_PASS", "1234567890123456",
                "SMTP_FROM", "user@gmail.com"
        )));
    }
}