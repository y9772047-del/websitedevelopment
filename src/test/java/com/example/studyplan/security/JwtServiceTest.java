package com.example.studyplan.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private final JwtService service = new JwtService("test-secret-with-at-least-thirty-two-bytes", 1800);

    @Test
    void issuedTokenIdentifiesItsUser() {
        assertEquals(42L, service.userId(service.issue(42L)));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = service.issue(42L);
        int signatureStart = token.lastIndexOf('.') + 1;
        // The first signature character changes decoded bytes; trailing padding bits may not.
        String tampered = token.substring(0, signatureStart)
                + (token.charAt(signatureStart) == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);
        assertThrows(JwtException.class, () -> service.userId(tampered));
    }
}
