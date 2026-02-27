package com.constructiq.backendjava.api;

import com.constructiq.backendjava.config.ConstructIQProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {
    private record Bucket(long minuteEpoch, int count) {}

    private final ConstructIQProperties properties;
    private final Map<String, Bucket> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(ConstructIQProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isRateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        long minute = Instant.now().getEpochSecond() / 60;

        int threshold = path.startsWith("/api/auth/login")
                ? properties.getAuthLoginRateLimitPerMinute()
                : properties.getRateLimitPerMinute();

        String key = ip + "|" + path;
        Bucket current = counters.get(key);
        if (current == null || current.minuteEpoch != minute) {
            current = new Bucket(minute, 0);
        }

        if (current.count >= threshold) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"Too many requests\",\"status\":429}");
            return;
        }

        counters.put(key, new Bucket(minute, current.count + 1));
        filterChain.doFilter(request, response);
    }
}
