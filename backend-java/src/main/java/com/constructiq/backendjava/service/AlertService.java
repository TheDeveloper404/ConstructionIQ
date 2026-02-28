package com.constructiq.backendjava.service;

import com.constructiq.backendjava.store.SqlDocumentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final SqlDocumentStore store;

    public AlertService(SqlDocumentStore store) {
        this.store = store;
    }

    @Async
    public void evaluateForProduct(String orgId, String productId, double newPrice) {
        try {
            List<Map<String, Object>> rules = store.find(
                    "alert_rules",
                    Map.of("org_id", orgId, "is_active", true),
                    null, false, 0, 100);

            for (Map<String, Object> rule : rules) {
                Map<String, Object> params = asMap(rule.get("params"));
                double thresholdPercent = asDouble(params.get("threshold_percent"), 10.0);
                int compareLastN = asInt(params.get("compare_last_n"), 3);

                List<Map<String, Object>> recentPrices = store.find(
                        "price_points",
                        Map.of("org_id", orgId, "normalized_product_id", productId),
                        "observed_at", true, 0, compareLastN + 1);

                if (recentPrices.size() < 2) continue;

                double lastPrice = asDouble(recentPrices.get(1).get("unit_price_normalized"), 0.0);
                if (lastPrice <= 0) continue;

                double changePercent = ((newPrice - lastPrice) / lastPrice) * 100.0;
                if (Math.abs(changePercent) >= thresholdPercent) {
                    String severity = Math.abs(changePercent) >= thresholdPercent * 2 ? "high" : "medium";

                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("new_price", newPrice);
                    payload.put("last_price", lastPrice);
                    payload.put("change_percent", Math.round(changePercent * 100.0) / 100.0);
                    payload.put("rule_name", asString(rule.get("name"), ""));

                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("id", UUID.randomUUID().toString());
                    event.put("org_id", orgId);
                    event.put("rule_id", rule.get("id"));
                    event.put("normalized_product_id", productId);
                    event.put("triggered_at", OffsetDateTime.now(ZoneOffset.UTC).toString());
                    event.put("severity", severity);
                    event.put("payload", payload);
                    event.put("status", "new");
                    store.upsert("alert_events", event);
                }
            }
        } catch (Exception e) {
            log.error("Failed to evaluate alerts for product {}: {}", productId, e.getMessage());
        }
    }

    private String asString(Object v, String fallback) {
        return v == null ? fallback : String.valueOf(v);
    }

    private double asDouble(Object v, double fallback) {
        if (v == null) return fallback;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return fallback; }
    }

    private int asInt(Object v, int fallback) {
        if (v == null) return fallback;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return fallback; }
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
