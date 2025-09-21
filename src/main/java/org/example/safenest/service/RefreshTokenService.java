package org.example.safenest.service;

import org.example.safenest.model.*;
import org.example.safenest.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repo;
    private final long refreshMs;

    public RefreshTokenService(RefreshTokenRepository repo, @Value("${REFRESH_TOKEN_EXPIRATION}") long refreshMs) {
        this.repo = repo;
        this.refreshMs = refreshMs;
    }

    @Transactional
    public String createToken(User user) {
        String raw = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String hash = sha256(raw);
        RefreshToken t = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiryDate(Instant.now().plusMillis(refreshMs))
                .createdAt(Instant.now())
                .build();
        repo.save(t);
        return raw;
    }

    public Optional<RefreshToken> findByRawToken(String raw) {
        return repo.findByTokenHash(sha256(raw));
    }

    @Transactional
    public void delete(RefreshToken token) {
        repo.delete(token);
    }

    @Transactional
    public void deleteAllForUser(User user) {
        repo.deleteAllByUser(user);
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    /** Утилита для хэша */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}