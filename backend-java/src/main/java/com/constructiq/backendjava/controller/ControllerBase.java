package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.security.AuthContext;
import com.constructiq.backendjava.security.AuthContextHolder;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public abstract class ControllerBase {

    protected final SqlDocumentStore store;
    protected final ConstructIQProperties properties;

    protected ControllerBase(SqlDocumentStore store, ConstructIQProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    protected DemoContext requireContext() {
        if (!properties.isDemoMode()) {
            AuthContext auth = AuthContextHolder.get();
            if (auth == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            return new DemoContext(auth.orgId(), auth.userId(), auth.role(), false);
        }
        return new DemoContext(properties.getDemoOrgId(), properties.getDemoUserId(), "admin", true);
    }

    protected void requireAdmin(DemoContext ctx) {
        if (!"admin".equalsIgnoreCase(ctx.userRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    protected Map<String, Object> sanitize(Map<String, Object> doc) {
        if (doc == null) return null;
        Map<String, Object> copy = new LinkedHashMap<>(doc);
        copy.remove("_id");
        return copy;
    }

    protected Map<String, Object> paginate(List<Map<String, Object>> items, long total, int page, int pageSize) {
        int totalPages = pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", total);
        out.put("page", page);
        out.put("page_size", pageSize);
        out.put("total_pages", totalPages);
        return out;
    }

    protected Map<String, Object> getOr404(String collection, String id, String orgId, String detail) {
        return store.findOne(collection, id, orgId)
                .map(this::sanitize)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, detail));
    }

    protected String requireNonBlank(Map<String, Object> data, String key, String message) {
        String value = asString(data.get(key), "").trim();
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    protected List<Object> requireNonEmptyList(Map<String, Object> data, String key, String message) {
        List<Object> values = asList(data.get(key));
        if (values.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return values;
    }

    protected Map<String, Object> baseOrgQuery(DemoContext ctx) {
        return new LinkedHashMap<>(Map.of("org_id", ctx.orgId()));
    }

    protected String nowIso() {
        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    protected String uuid() {
        return UUID.randomUUID().toString();
    }

    protected String asString(Object v, String fallback) {
        return v == null ? fallback : String.valueOf(v);
    }

    protected double asDouble(Object v, double fallback) {
        if (v == null) return fallback;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return fallback; }
    }

    protected int asInt(Object v, int fallback) {
        if (v == null) return fallback;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return fallback; }
    }

    @SuppressWarnings("unchecked")
    protected List<Object> asList(Object v) {
        if (v instanceof List<?> list) return (List<Object>) list;
        return new ArrayList<>();
    }

    protected Map<String, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return new LinkedHashMap<>();
    }
}
