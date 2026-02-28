package com.constructiq.backendjava.api;

import com.constructiq.backendjava.config.ConstructIQProperties;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {
    private record Bucket(long minuteEpoch, int count) {}

    private final ConstructIQProperties properties;
    private final MeterRegistry meterRegistry;
    private final Map<String, Bucket> counters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "rate-limit-cleaner"); t.setDaemon(true); return t; });

    public RateLimitFilter(ConstructIQProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        cleaner.scheduleAtFixedRate(this::evictExpiredBuckets, 1, 1, TimeUnit.MINUTES);
    }

    private void evictExpiredBuckets() {
        long currentMinute = Instant.now().getEpochSecond() / 60;
        counters.entrySet().removeIf(entry -> entry.getValue().minuteEpoch() < currentMinute);
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
            meterRegistry.counter("constructiq_rate_limit_blocked_total", "path", path).increment();
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"Too many requests\",\"status\":429}");
            return;
        }

        counters.put(key, new Bucket(minute, current.count + 1));
        meterRegistry.counter("constructiq_rate_limit_allowed_total", "path", path).increment();
        filterChain.doFilter(request, response);
    }
}
