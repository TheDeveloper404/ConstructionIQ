package com.constructiq.backendjava.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public String hashIfPlaintext(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (isBcrypt(value)) {
            return value;
        }
        return encoder.encode(value);
    }

    public boolean matches(String raw, String stored) {
        if (raw == null || stored == null || stored.isBlank()) {
            return false;
        }
        if (isBcrypt(stored)) {
            return encoder.matches(raw, stored);
        }
        return stored.equals(raw);
    }

    public boolean isBcrypt(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
