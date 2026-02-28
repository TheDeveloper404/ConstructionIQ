package com.constructiq.backendjava.service;

import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class QuoteService {

    private final SqlDocumentStore store;
    private final PricePointService pricePointService;
    private final AlertService alertService;

    public QuoteService(SqlDocumentStore store,
                        PricePointService pricePointService,
                        AlertService alertService) {
        this.store = store;
        this.pricePointService = pricePointService;
        this.alertService = alertService;
    }

    public Map<String, Object> buildAndPersist(DemoContext ctx, Map<String, Object> data) {
        requireNonBlank(data, "supplier_id", "Quote supplier_id is required");
        Map<String, Object> quote = new LinkedHashMap<>(data);

        List<Map<String, Object>> items = new ArrayList<>();
        double totalAmount = 0.0;
        for (Object raw : requireNonEmptyList(quote, "items", "Quote must contain at least one item")) {
            Map<String, Object> item = asMap(raw);
            requireNonBlank(item, "raw_line_text", "Quote item raw_line_text is required");
            item.putIfAbsent("id", UUID.randomUUID().toString());
            double qty = asDouble(item.get("qty"), 0.0);
            double unitPrice = asDouble(item.get("unit_price"), 0.0);
            if (qty <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote item qty must be greater than 0");
            }
            if (unitPrice < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote item unit_price must be >= 0");
            }
            item.put("total_price", qty * unitPrice);
            items.add(item);
            totalAmount += qty * unitPrice;
        }

        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();
        quote.put("id", UUID.randomUUID().toString());
        quote.put("org_id", ctx.orgId());
        quote.putIfAbsent("status", "received");
        quote.putIfAbsent("currency", "USD");
        quote.putIfAbsent("attachments", new ArrayList<>());
        quote.put("items", items);
        quote.put("total_amount", totalAmount);
        quote.put("received_at", now);
        quote.put("created_at", now);
        quote.put("updated_at", now);

        store.upsert("quotes", quote);

        for (Map<String, Object> item : items) {
            String productId = asString(item.get("normalized_product_id"), "");
            if (!productId.isBlank() && !"null".equalsIgnoreCase(productId)) {
                pricePointService.createFromQuoteItem(ctx, quote, item, productId);
                alertService.evaluateForProduct(ctx.orgId(), productId, asDouble(item.get("unit_price"), 0.0));
            }
        }

        return sanitize(quote);
    }

    private Map<String, Object> sanitize(Map<String, Object> doc) {
        if (doc == null) return null;
        Map<String, Object> copy = new LinkedHashMap<>(doc);
        copy.remove("_id");
        return copy;
    }

    private String requireNonBlank(Map<String, Object> data, String key, String message) {
        String value = asString(data.get(key), "").trim();
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private List<Object> requireNonEmptyList(Map<String, Object> data, String key, String message) {
        List<Object> values = asList(data.get(key));
        if (values.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return values;
    }

    private String asString(Object v, String fallback) {
        return v == null ? fallback : String.valueOf(v);
    }

    private double asDouble(Object v, double fallback) {
        if (v == null) return fallback;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return fallback; }
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object v) {
        if (v instanceof List<?> list) return (List<Object>) list;
        return new ArrayList<>();
    }

    private Map<String, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
            return out;
        }
        return new LinkedHashMap<>();
    }
}
