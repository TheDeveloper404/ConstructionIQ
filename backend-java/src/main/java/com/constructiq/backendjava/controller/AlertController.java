package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController extends ControllerBase {

    private static final Set<String> ALERT_EVENT_STATUSES = Set.of("new", "ack", "resolved");

    public AlertController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping("/rules")
    public Map<String, Object> listAlertRules() {
        DemoContext ctx = requireContext();
        List<Map<String, Object>> rules = store.find(
                "alert_rules", Map.of("org_id", ctx.orgId()), null, false, 0, 100);
        return Map.of("rules", rules);
    }

    @PostMapping("/rules")
    public Map<String, Object> createAlertRule(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        requireNonBlank(data, "name", "Alert rule name is required");
        requireNonBlank(data, "type", "Alert rule type is required");
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.putIfAbsent("is_active", true);
        doc.putIfAbsent("params", new LinkedHashMap<>());
        doc.put("created_at", nowIso());
        store.upsert("alert_rules", doc);
        return sanitize(doc);
    }

    @PutMapping("/rules/{ruleId}")
    public Map<String, Object> updateAlertRule(@PathVariable String ruleId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("alert_rules", ruleId, ctx.orgId(), "Rule not found");
        Map<String, Object> updates = new LinkedHashMap<>(data);
        updates.remove("id");
        updates.remove("org_id");
        store.updateByQuery("alert_rules", Map.of("id", ruleId), updates, true);
        return getOr404("alert_rules", ruleId, ctx.orgId(), "Rule not found");
    }

    @DeleteMapping("/rules/{ruleId}")
    public Map<String, Object> deleteAlertRule(@PathVariable String ruleId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("alert_rules", ruleId, ctx.orgId());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found");
        return Map.of("message", "Rule deleted");
    }

    @GetMapping("/events")
    public Map<String, Object> listAlertEvents(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String status) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("org_id", ctx.orgId());
        if (status != null && !status.isBlank()) query.put("status", status);

        long total = store.count("alert_events", query);
        List<Map<String, Object>> events = store.find(
                "alert_events", query, "triggered_at", true, (page - 1) * pageSize, pageSize);

        Set<String> productIds = new HashSet<>();
        for (Map<String, Object> e : events) {
            String pid = asString(e.get("normalized_product_id"), "");
            if (!pid.isBlank()) productIds.add(pid);
        }

        Map<String, String> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Map<String, Object>> products = store.find(
                    "normalized_products",
                    Map.of("org_id", ctx.orgId(), "id", Map.of("$in", new ArrayList<>(productIds))),
                    null, false, 0, 100);
            for (Map<String, Object> p : products) {
                productMap.put(asString(p.get("id"), ""), asString(p.get("canonical_name"), "Unknown"));
            }
        }

        for (Map<String, Object> event : events) {
            String pid = asString(event.get("normalized_product_id"), "");
            event.put("product_name", productMap.getOrDefault(pid, "Unknown"));
        }

        return paginate(events, total, page, pageSize);
    }

    @PutMapping("/events/{eventId}")
    public Map<String, Object> updateAlertEvent(@PathVariable String eventId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        String newStatus = asString(data.get("status"), "").trim();
        if (!ALERT_EVENT_STATUSES.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Allowed values: " + ALERT_EVENT_STATUSES);
        }
        Map<String, Object> event = getOr404("alert_events", eventId, ctx.orgId(), "Event not found");
        event.put("status", newStatus);
        store.upsert("alert_events", event);
        return Map.of("message", "Event updated");
    }
}
