package com.nexolab.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

public class JwtUtil {
    // Use a persistent secret from environment to ensure JWTs remain valid across restarts.
    private static final String SECRET_ENV = System.getenv("JWT_SECRET");
    private static final String DEFAULT_SECRET = "change_this_default_secret_to_a_long_random_value_please";
    private static final Key key;

    static {
        String secret = SECRET_ENV;
        if (secret == null || secret.trim().isEmpty()) {
            // Fallback to default but warn in logs (can't use logging framework here easily)
            secret = DEFAULT_SECRET;
            System.err.println("WARNING: JWT_SECRET not set. Using default insecure secret. Set JWT_SECRET env var to a secure value.");
        }

        // If the secret looks like base64, decode it, otherwise use the raw bytes.
        byte[] keyBytes;
        try {
            // Try base64 decode
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        key = Keys.hmacShaKeyFor(keyBytes);
    }
    private static final long EXPIRATION_TIME = 86400000; // 1 day

    public static String generateToken(Long userId) {
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return Long.parseLong(claims.getSubject());
    }
}