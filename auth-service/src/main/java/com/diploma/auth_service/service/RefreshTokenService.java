package com.diploma.auth_service.service;

import com.diploma.auth_service.entity.RefreshToken;
import com.diploma.auth_service.entity.User;
import com.diploma.auth_service.exception.InvalidTokenException;
import com.diploma.auth_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository repository;

    @Value("${app.jwt.refresh-token-ttl}")
    private Duration refreshTtl;

    @Transactional
    public RefreshToken issue(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(generateOpaqueToken());
        token.setExpiresAt(Instant.now().plus(refreshTtl));
        return repository.save(token);
    }

    @Transactional
    public RefreshToken rotate(String rawToken) {
        RefreshToken existing = repository.findByToken(rawToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!existing.isUsable()) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        existing.setRevoked(true);
        return issue(existing.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByToken(rawToken).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllByUserId(userId);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
