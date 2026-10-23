package com.example.studyplan.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "study-plan-system";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long ttlSeconds;

    public JwtService(@Value("${auth.jwt.secret:}") String configuredSecret,
                      @Value("${auth.jwt.ttl-seconds:1800}") long ttlSeconds) {
        byte[] keyBytes;
        if (configuredSecret == null || configuredSecret.isBlank()) {
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("JWT_SECRET is absent; using an ephemeral signing key. Tokens will expire after restart.");
        } else {
            keyBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
            }
        }
        if (ttlSeconds <= 0 || ttlSeconds > 86400) {
            throw new IllegalArgumentException("JWT TTL must be between 1 and 86400 seconds");
        }
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        this.decoder = jwtDecoder;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(Long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public Long userId(String token) {
        try {
            return Long.valueOf(decoder.decode(token).getSubject());
        } catch (NumberFormatException exception) {
            throw new BadJwtException("Invalid user id", exception);
        }
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}
