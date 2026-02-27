package com.constructiq.backendjava.security;

public record AuthContext(String orgId, String userId, String role, String email) {
}
