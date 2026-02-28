package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController extends ControllerBase {

    public HealthController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "healthy", "timestamp", nowIso());
    }

    @GetMapping("/ready")
    public Map<String, Object> ready() {
        try {
            store.count("organizations", Map.of());
            return Map.of("status", "ready", "database", "connected");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Database not ready: " + e.getMessage());
        }
    }
}
