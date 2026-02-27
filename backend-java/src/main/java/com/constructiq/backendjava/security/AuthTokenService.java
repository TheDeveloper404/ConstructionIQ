package com.constructiq.backendjava.security;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthTokenService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ConstructIQProperties properties;
    private final ObjectMapper mapper;

    public AuthTokenService(ConstructIQProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public String createToken(String userId, String orgId, String role, String email) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", userId);
            payload.put("org", orgId);
            payload.put("role", role);
            payload.put("email", email);
            payload.put("exp", Instant.now().plusSeconds(properties.getAuthTokenTtlMinutes() * 60).getEpochSecond());

            String encodedHeader = b64Url(mapper.writeValueAsBytes(header));
            String encodedPayload = b64Url(mapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = b64Url(sign(signingInput));
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create auth token", e);
        }
    }

    public Optional<AuthContext> parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String signingInput = parts[0] + "." + parts[1];
            if (!isSignatureValid(signingInput, parts[2])) {
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = mapper.readValue(payloadBytes, MAP_TYPE);

            long exp = Long.parseLong(String.valueOf(payload.getOrDefault("exp", 0)));
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }

            String userId = String.valueOf(payload.getOrDefault("sub", ""));
            String orgId = String.valueOf(payload.getOrDefault("org", ""));
            String role = String.valueOf(payload.getOrDefault("role", "buyer"));
            String email = String.valueOf(payload.getOrDefault("email", ""));
            if (userId.isBlank() || orgId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AuthContext(orgId, userId, role, email));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private byte[] sign(String content) throws Exception {
        return sign(content, properties.getAuthTokenSecret());
    }

    private byte[] sign(String content, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isSignatureValid(String signingInput, String incomingSignature) throws Exception {
        for (String secret : allValidSecrets()) {
            String expectedSignature = b64Url(sign(signingInput, secret));
            if (expectedSignature.equals(incomingSignature)) {
                return true;
            }
        }
        return false;
    }

    private Iterable<String> allValidSecrets() {
        ArrayList<String> secrets = new ArrayList<>();
        secrets.add(properties.getAuthTokenSecret());

        String previous = properties.getAuthTokenPreviousSecrets();
        if (previous != null && !previous.isBlank()) {
            for (String candidate : previous.split(",")) {
                String value = candidate.trim();
                if (!value.isBlank()) {
                    secrets.add(value);
                }
            }
        }
        return secrets;
    }

    private String b64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
