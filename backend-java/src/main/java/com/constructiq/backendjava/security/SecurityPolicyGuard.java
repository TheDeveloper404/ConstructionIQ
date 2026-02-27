package com.constructiq.backendjava.security;

import com.constructiq.backendjava.config.ConstructIQProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityPolicyGuard {
    private static final Logger log = LoggerFactory.getLogger(SecurityPolicyGuard.class);

    private final ConstructIQProperties properties;
    private final PasswordService passwordService;

    public SecurityPolicyGuard(ConstructIQProperties properties, PasswordService passwordService) {
        this.properties = properties;
        this.passwordService = passwordService;
    }

    @PostConstruct
    public void checkPolicy() {
        String secret = properties.getAuthTokenSecret();
        if (secret == null || secret.length() < 24 || "change-this-secret".equals(secret)) {
            log.warn("Security policy: AUTH_TOKEN_SECRET is weak/default. Use >=24 chars and rotate regularly.");
        }

        if (!passwordService.isBcrypt(properties.getAdminPassword())) {
            log.warn("Security policy: ADMIN_PASSWORD is plaintext. Prefer bcrypt hash.");
        }
    }
}
