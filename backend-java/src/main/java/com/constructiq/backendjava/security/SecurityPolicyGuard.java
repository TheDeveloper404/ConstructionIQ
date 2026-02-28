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
        boolean weakSecret = secret == null || secret.length() < 24 || "change-this-secret".equals(secret);
        boolean plaintextPassword = !passwordService.isBcrypt(properties.getAdminPassword());

        if (properties.isDemoMode()) {
            if (weakSecret) {
                log.warn("Security policy [DEMO]: AUTH_TOKEN_SECRET is weak/default. Set a strong secret before going to production.");
            }
            if (plaintextPassword) {
                log.warn("Security policy [DEMO]: ADMIN_PASSWORD is plaintext. Use a bcrypt hash before going to production.");
            }
        } else {
            if (weakSecret) {
                throw new IllegalStateException(
                        "Security policy violation: AUTH_TOKEN_SECRET is weak or default. "
                        + "Set AUTH_TOKEN_SECRET to a random string of at least 24 characters.");
            }
            if (plaintextPassword) {
                throw new IllegalStateException(
                        "Security policy violation: ADMIN_PASSWORD is stored as plaintext. "
                        + "Set ADMIN_PASSWORD to a bcrypt hash (cost 12).");
            }
        }
    }
}
