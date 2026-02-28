package com.constructiq.backendjava.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {

    private final PasswordService service = new PasswordService();

    @Test
    void hashIfPlaintext_returnsNonBlankBcryptHash() {
        String hash = service.hashIfPlaintext("mypassword");
        assertTrue(service.isBcrypt(hash));
    }

    @Test
    void hashIfPlaintext_idempotentOnBcryptInput() {
        String hash = service.hashIfPlaintext("mypassword");
        String rehashed = service.hashIfPlaintext(hash);
        assertEquals(hash, rehashed);
    }

    @Test
    void hashIfPlaintext_nullOrBlank_returnsEmpty() {
        assertEquals("", service.hashIfPlaintext(null));
        assertEquals("", service.hashIfPlaintext(""));
        assertEquals("", service.hashIfPlaintext("   "));
    }

    @Test
    void matches_validPassword_returnsTrue() {
        String hash = service.hashIfPlaintext("correct");
        assertTrue(service.matches("correct", hash));
    }

    @Test
    void matches_wrongPassword_returnsFalse() {
        String hash = service.hashIfPlaintext("correct");
        assertFalse(service.matches("wrong", hash));
    }

    @Test
    void matches_nullRawOrStored_returnsFalse() {
        String hash = service.hashIfPlaintext("password");
        assertFalse(service.matches(null, hash));
        assertFalse(service.matches("password", null));
        assertFalse(service.matches("password", ""));
    }

    @Test
    void matches_plaintextStored_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> service.matches("password", "plaintext"));
    }

    @Test
    void isBcrypt_detectsAllPrefixes() {
        assertTrue(service.isBcrypt("$2a$12$abc"));
        assertTrue(service.isBcrypt("$2b$12$abc"));
        assertTrue(service.isBcrypt("$2y$12$abc"));
        assertFalse(service.isBcrypt("plaintext"));
        assertFalse(service.isBcrypt(null));
    }
}
