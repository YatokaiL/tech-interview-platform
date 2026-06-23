package com.diploma.notification_service.client;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Mints a short-lived service JWT signed with the shared HS256 secret so the
 * notification-service (a Kafka consumer with no user context) can call other
 * services' authenticated endpoints. Acts as an ADMIN service account.
 */
@Component
public class InternalTokenProvider {

    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final SecretKey key;

    public InternalTokenProvider(@Value("${app.jwt.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String mint() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(SYSTEM_USER_ID.toString())
                .claim("email", "notification-service@interview.local")
                .claim("role", "ADMIN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(120)))
                .signWith(key)
                .compact();
    }
}
