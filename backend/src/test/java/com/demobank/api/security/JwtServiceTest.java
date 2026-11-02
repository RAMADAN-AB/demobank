package com.demobank.api.security;

import com.demobank.api.model.Role;
import com.demobank.api.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Cles de test uniquement (256 bits et plus, encodees en Base64)
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1kZW1vYmFuay10ZXN0cy0yNTZiaXQ=";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWZvci1kZW1vYmFuay10ZXN0cy0yNTY=";

    private JwtService jwtService;

    private final User alice = user("alice@demobank.test");
    private final User bob = user("bob@demobank.test");

    @BeforeEach
    void setUp() {
        jwtService = newJwtService(SECRET, 3_600_000L);
    }

    @Test
    void generatedTokenCarriesUsernameAsSubject() {
        String token = jwtService.generateToken(alice);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@demobank.test");
    }

    @Test
    void tokenIsValidForItsOwner() {
        String token = jwtService.generateToken(alice);

        assertThat(jwtService.isTokenValid(token, alice)).isTrue();
    }

    @Test
    void tokenIsInvalidForAnotherUser() {
        String token = jwtService.generateToken(alice);

        assertThat(jwtService.isTokenValid(token, bob)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiring = newJwtService(SECRET, -1_000L);
        String token = expiring.generateToken(alice);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, alice))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = newJwtService(OTHER_SECRET, 3_600_000L).generateToken(alice);

        assertThatThrownBy(() -> jwtService.extractUsername(forged))
                .isInstanceOf(SignatureException.class);
    }

    private static JwtService newJwtService(String secret, long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "jwtExpiration", expiration);
        return service;
    }

    private static User user(String email) {
        return User.builder().email(email).password("x").role(Role.ROLE_USER).build();
    }
}
