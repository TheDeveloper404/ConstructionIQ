package com.constructiq.backendjava.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final AuthTokenService tokenService;
    private final MeterRegistry meterRegistry;

    public AuthFilter(AuthTokenService tokenService, MeterRegistry meterRegistry) {
        this.tokenService = tokenService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                tokenService.parseToken(token).ifPresentOrElse(
                        ctx -> {
                            meterRegistry.counter("constructiq_auth_token_total", "outcome", "valid").increment();
                            AuthContextHolder.set(ctx);
                        },
                        () -> meterRegistry.counter("constructiq_auth_token_total", "outcome", "invalid").increment()
                );
            }
            filterChain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
        }
    }
}
