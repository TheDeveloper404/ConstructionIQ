package com.constructiq.backendjava.security;

import com.constructiq.backendjava.config.ConstructIQProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthTokenService {

    private final ConstructIQProperties properties;

    public AuthTokenService(ConstructIQProperties properties) {
        this.properties = properties;
    }

    public String createToken(String userId, String orgId, String role, String email) {
        long ttlMs = (long) properties.getAuthTokenTtlMinutes() * 60 * 1000;
        return Jwts.builder()
                .subject(userId)
                .claim("org", orgId)
                .claim("role", role)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(signingKey(properties.getAuthTokenSecret()), Jwts.SIG.HS256)
                .compact();
    }

    public Optional<AuthContext> parseToken(String token) {
        for (String secret : allValidSecrets()) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey(secret))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String orgId = claims.get("org", String.class);
                String role = claims.getOrDefault("role", "buyer").toString();
                String email = claims.getOrDefault("email", "").toString();

                if (userId == null || userId.isBlank() || orgId == null || orgId.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(new AuthContext(orgId, userId, role, email));
            } catch (JwtException | IllegalArgumentException ignored) {
                // try next secret
            }
        }
        return Optional.empty();
    }

    private SecretKey signingKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private List<String> allValidSecrets() {
        List<String> secrets = new ArrayList<>();
        secrets.add(properties.getAuthTokenSecret());
        String previous = properties.getAuthTokenPreviousSecrets();
        if (previous != null && !previous.isBlank()) {
            for (String candidate : previous.split(",")) {
                String value = candidate.trim();
                if (!value.isBlank()) secrets.add(value);
            }
        }
        return secrets;
    }
}
