package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.security.AuthTokenService;
import com.constructiq.backendjava.security.PasswordService;
import com.constructiq.backendjava.store.SqlDocumentStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController extends ControllerBase {

    private final AuthTokenService tokenService;
    private final PasswordService passwordService;
    private final MeterRegistry meterRegistry;

    public AuthController(SqlDocumentStore store,
                          ConstructIQProperties properties,
                          AuthTokenService tokenService,
                          PasswordService passwordService,
                          MeterRegistry meterRegistry) {
        super(store, properties);
        this.tokenService = tokenService;
        this.passwordService = passwordService;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> data) {
        String email = asString(data.get("email"), "").trim().toLowerCase(Locale.ROOT);
        String password = asString(data.get("password"), "");
        if (email.isBlank() || password.isBlank()) {
            metric("bad_request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        boolean defaultAdmin = email.equalsIgnoreCase(properties.getAdminEmail())
                && passwordService.matches(password, properties.getAdminPassword());

        String orgId = properties.getDemoOrgId();
        String userId = properties.getDemoUserId();
        String role = "admin";

        if (!defaultAdmin) {
            List<Map<String, Object>> users = store.find(
                    "users", Map.of("email", email, "status", "active"),
                    null, false, 0, 1);
            if (users.isEmpty()) {
                metric("unauthorized");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            Map<String, Object> user = users.get(0);
            String storedPassword = asString(user.get("password"), "");
            if (!passwordService.matches(password, storedPassword)) {
                metric("unauthorized");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            orgId = asString(user.get("org_id"), orgId);
            userId = asString(user.get("id"), userId);
            role = asString(user.get("role"), "buyer");
        }

        String token = tokenService.createToken(userId, orgId, role, email);
        metric("success");
        return Map.of(
                "access_token", token,
                "token_type", "bearer",
                "user", Map.of("id", userId, "org_id", orgId, "role", role, "email", email),
                "demo_mode", properties.isDemoMode()
        );
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        var ctx = requireContext();
        return Map.of(
                "id", ctx.userId(),
                "org_id", ctx.orgId(),
                "role", ctx.userRole(),
                "demo_mode", ctx.demo()
        );
    }

    private void metric(String outcome) {
        meterRegistry.counter("constructiq_auth_login_total", "outcome", outcome).increment();
    }
}
