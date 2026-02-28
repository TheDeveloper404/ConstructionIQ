package com.constructiq.backendjava.security;

import com.constructiq.backendjava.config.ConstructIQProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenServiceTest {

    private ConstructIQProperties props;
    private AuthTokenService service;

    @BeforeEach
    void setUp() {
        props = new ConstructIQProperties();
        props.setAuthTokenSecret("a-strong-test-secret-at-least-32chars!!");
        props.setAuthTokenTtlMinutes(60);
        service = new AuthTokenService(props);
    }

    @Test
    void createToken_producesValidJwt() {
        String token = service.createToken("user-1", "org-1", "admin", "test@test.com");
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "JWT must have 3 parts");
    }

    @Test
    void parseToken_validToken_returnsAuthContext() {
        String token = service.createToken("user-1", "org-1", "admin", "test@test.com");
        Optional<AuthContext> ctx = service.parseToken(token);

        assertTrue(ctx.isPresent());
        assertEquals("user-1", ctx.get().userId());
        assertEquals("org-1", ctx.get().orgId());
        assertEquals("admin", ctx.get().role());
        assertEquals("test@test.com", ctx.get().email());
    }

    @Test
    void parseToken_tamperedToken_returnsEmpty() {
        String token = service.createToken("user-1", "org-1", "admin", "test@test.com");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertTrue(service.parseToken(tampered).isEmpty());
    }

    @Test
    void parseToken_expiredToken_returnsEmpty() {
        props.setAuthTokenTtlMinutes(0);
        AuthTokenService shortLived = new AuthTokenService(props);
        String token = shortLived.createToken("user-1", "org-1", "admin", "test@test.com");
        assertTrue(shortLived.parseToken(token).isEmpty());
    }

    @Test
    void parseToken_wrongSecret_returnsEmpty() {
        String token = service.createToken("user-1", "org-1", "admin", "test@test.com");
        ConstructIQProperties otherProps = new ConstructIQProperties();
        otherProps.setAuthTokenSecret("completely-different-secret-32chars!!");
        otherProps.setAuthTokenTtlMinutes(60);
        AuthTokenService other = new AuthTokenService(otherProps);
        assertTrue(other.parseToken(token).isEmpty());
    }

    @Test
    void parseToken_previousSecretStillValid() {
        String oldSecret = "old-secret-used-before-rotation-32c!!";
        String newSecret = "new-secret-after-rotation-at-32chars!!";

        ConstructIQProperties oldProps = new ConstructIQProperties();
        oldProps.setAuthTokenSecret(oldSecret);
        oldProps.setAuthTokenTtlMinutes(60);
        AuthTokenService oldService = new AuthTokenService(oldProps);
        String tokenSignedWithOld = oldService.createToken("u1", "o1", "buyer", "x@x.com");

        props.setAuthTokenSecret(newSecret);
        props.setAuthTokenPreviousSecrets(oldSecret);
        AuthTokenService rotated = new AuthTokenService(props);

        assertTrue(rotated.parseToken(tokenSignedWithOld).isPresent());
    }

    @Test
    void parseToken_malformedToken_returnsEmpty() {
        assertTrue(service.parseToken("not.a.token").isEmpty());
        assertTrue(service.parseToken("abc").isEmpty());
        assertTrue(service.parseToken("a.b.c").isEmpty());
    }
}
