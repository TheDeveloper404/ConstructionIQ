package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/price-history")
public class PriceHistoryController extends ControllerBase {

    public PriceHistoryController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping
    public Map<String, Object> getPriceHistory(@RequestParam(required = false) String product_id,
                                               @RequestParam(required = false) String supplier_id,
                                               @RequestParam(defaultValue = "90") int days) {
        DemoContext ctx = requireContext();
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("org_id", ctx.orgId());
        query.put("observed_at", Map.of("$gte", cutoff.toString()));
        if (product_id != null && !product_id.isBlank()) query.put("normalized_product_id", product_id);
        if (supplier_id != null && !supplier_id.isBlank()) query.put("supplier_id", supplier_id);

        List<Map<String, Object>> pricePoints = store.find("price_points", query, "observed_at", false, 0, 1000);
        return Map.of("price_points", pricePoints);
    }

    @GetMapping("/product/{productId}")
    public Map<String, Object> getProductPriceHistory(@PathVariable String productId,
                                                      @RequestParam(defaultValue = "90") int days) {
        DemoContext ctx = requireContext();
        Map<String, Object> product = getOr404("normalized_products", productId, ctx.orgId(), "Product not found");

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("org_id", ctx.orgId());
        query.put("normalized_product_id", productId);
        query.put("observed_at", Map.of("$gte", cutoff.toString()));

        List<Map<String, Object>> pricePoints = store.find("price_points", query, "observed_at", false, 0, 1000);

        Set<String> supplierIds = new HashSet<>();
        for (Map<String, Object> pp : pricePoints) {
            String sid = asString(pp.get("supplier_id"), "");
            if (!sid.isBlank()) supplierIds.add(sid);
        }

        Map<String, String> supplierMap = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            List<Map<String, Object>> suppliers = store.find(
                    "suppliers",
                    Map.of("org_id", ctx.orgId(), "id", Map.of("$in", new ArrayList<>(supplierIds))),
                    null, false, 0, 100);
            for (Map<String, Object> s : suppliers) {
                supplierMap.put(asString(s.get("id"), ""), asString(s.get("name"), "Unknown"));
            }
        }

        for (Map<String, Object> pp : pricePoints) {
            String sid = asString(pp.get("supplier_id"), "");
            if (!sid.isBlank()) pp.put("supplier_name", supplierMap.getOrDefault(sid, "Unknown"));
        }

        return Map.of("product", product, "price_points", pricePoints);
    }
}
