package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.security.PasswordService;
import com.constructiq.backendjava.store.SqlDocumentStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/demo")
public class DemoController extends ControllerBase {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final PasswordService passwordService;

    public DemoController(SqlDocumentStore store,
                          ConstructIQProperties properties,
                          PasswordService passwordService) {
        super(store, properties);
        this.passwordService = passwordService;
    }

    @PostConstruct
    public void startupSeed() {
        try {
            seedDemoData();
        } catch (Exception e) {
            log.warn("Skipping startup demo seed: {}", e.getMessage());
        }
    }

    @GetMapping("/status")
    public Map<String, Object> demoStatus() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("demo_mode", properties.isDemoMode());
        out.put("demo_org_id", properties.isDemoMode() ? properties.getDemoOrgId() : null);
        out.put("demo_user_id", properties.isDemoMode() ? properties.getDemoUserId() : null);
        return out;
    }

    @PostMapping("/reset")
    public Map<String, Object> resetDemo() {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        List<String> collections = List.of("projects", "suppliers", "normalized_products",
                "rfqs", "quotes", "price_points", "alert_rules", "alert_events");
        for (String coll : collections) {
            store.deleteByQuery(coll, Map.of("org_id", ctx.orgId()), false);
        }
        seedDemoData();
        return Map.of("message", "Demo data reset successfully");
    }

    @PostMapping("/__seed")
    public Map<String, Object> seedEndpoint() {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        seedDemoData();
        return Map.of("message", "seeded");
    }

    public void seedDemoData() {
        if (!properties.isDemoMode()) return;

        String orgId = properties.getDemoOrgId();
        String userId = properties.getDemoUserId();

        if (store.findOne("organizations", orgId).isEmpty()) {
            store.upsert("organizations", new LinkedHashMap<>(Map.of(
                    "id", orgId,
                    "name", "Demo Construction Co.",
                    "plan", "enterprise",
                    "currency", "USD",
                    "created_at", nowIso()
            )));
        }

        if (store.findOne("users", userId).isEmpty()) {
            store.upsert("users", new LinkedHashMap<>(Map.of(
                    "id", userId,
                    "org_id", orgId,
                    "email", "demo@constructiq.com",
                    "name", "Demo Admin",
                    "role", "admin",
                    "password", passwordService.hashIfPlaintext("demo123"),
                    "status", "active",
                    "created_at", nowIso()
            )));
        } else {
            Map<String, Object> existingUser = store.findOne("users", userId).orElse(new LinkedHashMap<>());
            String existingPassword = asString(existingUser.get("password"), "");
            if (existingPassword.isBlank()) {
                store.updateByQuery(
                        "users",
                        Map.of("id", userId, "org_id", orgId),
                        Map.of("password", passwordService.hashIfPlaintext("demo123"), "updated_at", nowIso()),
                        true
                );
            }
        }

        if (store.count("projects", Map.of("org_id", orgId)) == 0) {
            List<Map<String, Object>> demoProjects = List.of(
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Downtown Office Tower",
                            "location", "New York, NY", "status", "active",
                            "description", "45-story commercial building", "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Riverside Residential",
                            "location", "Chicago, IL", "status", "active",
                            "description", "Mixed-use residential complex", "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Industrial Park Phase 2",
                            "location", "Houston, TX", "status", "on_hold",
                            "description", "Warehouse and logistics facility", "created_at", nowIso(), "updated_at", nowIso()))
            );
            demoProjects.forEach(d -> store.upsert("projects", d));
        }

        if (store.count("suppliers", Map.of("org_id", orgId)) == 0) {
            List<Map<String, Object>> demoSuppliers = List.of(
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Steel Solutions Inc.",
                            "contact_email", "sales@steelsolutions.com", "phone", "+1-555-0101",
                            "tags", List.of("steel", "structural"), "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Concrete Masters LLC",
                            "contact_email", "orders@concretemasters.com", "phone", "+1-555-0102",
                            "tags", List.of("concrete", "cement"), "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Electrical Supplies Co.",
                            "contact_email", "info@electricalsupplies.com", "phone", "+1-555-0103",
                            "tags", List.of("electrical", "wiring"), "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Plumbing Pro Distributors",
                            "contact_email", "orders@plumbingpro.com", "phone", "+1-555-0104",
                            "tags", List.of("plumbing", "pipes"), "created_at", nowIso(), "updated_at", nowIso()))
            );
            demoSuppliers.forEach(d -> store.upsert("suppliers", d));
        }

        if (store.count("normalized_products", Map.of("org_id", orgId)) == 0) {
            List<Map<String, Object>> demoProducts = List.of(
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "Rebar #4 Grade 60",
                            "category", "Steel", "base_uom", "ton", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "Ready-Mix Concrete 4000 PSI",
                            "category", "Concrete", "base_uom", "cubic_yard", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "12/2 Romex Wire",
                            "category", "Electrical", "base_uom", "ft", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "3/4\" Copper Pipe Type L",
                            "category", "Plumbing", "base_uom", "ft", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "Portland Cement Type I/II",
                            "category", "Concrete", "base_uom", "bag", "attributes", new LinkedHashMap<>(), "created_at", nowIso()))
            );
            demoProducts.forEach(d -> store.upsert("normalized_products", d));
        }
    }
}
