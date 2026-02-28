package com.constructiq.backendjava.service;

import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PricePointService {

    private final SqlDocumentStore store;

    public PricePointService(SqlDocumentStore store) {
        this.store = store;
    }

    public void createFromQuoteItem(DemoContext ctx, Map<String, Object> quote, Map<String, Object> item, String productId) {
        Map<String, Object> pp = new LinkedHashMap<>();
        pp.put("id", UUID.randomUUID().toString());
        pp.put("org_id", ctx.orgId());
        pp.put("normalized_product_id", productId);
        pp.put("source_type", "quote");
        pp.put("source_id", quote.get("id"));
        pp.put("observed_at", OffsetDateTime.now(ZoneOffset.UTC).toString());
        pp.put("currency", asString(quote.get("currency"), "USD"));
        pp.put("unit_price_normalized", asDouble(item.get("unit_price"), 0.0));
        pp.put("uom_normalized", asString(item.get("uom"), ""));
        pp.put("supplier_id", quote.get("supplier_id"));
        pp.put("meta", new LinkedHashMap<>());
        store.upsert("price_points", pp);
    }

    private String asString(Object v, String fallback) {
        return v == null ? fallback : String.valueOf(v);
    }

    private double asDouble(Object v, double fallback) {
        if (v == null) return fallback;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return fallback; }
    }
}
