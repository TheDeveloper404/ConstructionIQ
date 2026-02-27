package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.security.AuthContext;
import com.constructiq.backendjava.security.AuthContextHolder;
import com.constructiq.backendjava.security.AuthTokenService;
import com.constructiq.backendjava.store.SqlDocumentStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final SqlDocumentStore store;
    private final ConstructIQProperties properties;
    private final AuthTokenService tokenService;

    public ApiController(SqlDocumentStore store, ConstructIQProperties properties, AuthTokenService tokenService) {
        this.store = store;
        this.properties = properties;
        this.tokenService = tokenService;
    }

    @PostConstruct
    public void startupSeed() {
        try {
            seedDemoData();
        } catch (Exception e) {
            log.warn("Skipping startup demo seed: {}", e.getMessage());
        }
    }

    private String nowIso() {
        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    private String uuid() {
        return UUID.randomUUID().toString();
    }

    private DemoContext requireContext() {
        if (!properties.isDemoMode()) {
            AuthContext auth = AuthContextHolder.get();
            if (auth == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            return new DemoContext(auth.orgId(), auth.userId(), auth.role(), false);
        }
        return new DemoContext(properties.getDemoOrgId(), properties.getDemoUserId(), "admin", true);
    }

    private void requireAdmin(DemoContext ctx) {
        if (!"admin".equalsIgnoreCase(ctx.userRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    private Map<String, Object> sanitize(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        Map<String, Object> copy = new LinkedHashMap<>(doc);
        copy.remove("_id");
        return copy;
    }

    private Map<String, Object> paginate(List<Map<String, Object>> items, long total, int page, int pageSize) {
        int totalPages = pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", total);
        out.put("page", page);
        out.put("page_size", pageSize);
        out.put("total_pages", totalPages);
        return out;
    }

    private String asString(Object v, String fallback) {
        return v == null ? fallback : String.valueOf(v);
    }

    private double asDouble(Object v, double fallback) {
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return fallback;
        }
    }

    private int asInt(Object v, int fallback) {
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object v) {
        if (v instanceof List<?> list) {
            return (List<Object>) list;
        }
        return new ArrayList<>();
    }

    private Map<String, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> baseOrgQuery(DemoContext ctx) {
        return new LinkedHashMap<>(Map.of("org_id", ctx.orgId()));
    }

    private Map<String, Object> getOr404(String collection, String id, String orgId, String detail) {
        return store.findOne(collection, id, orgId)
                .map(this::sanitize)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, detail));
    }

    // ==================== DEMO ====================
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> data) {
        String email = asString(data.get("email"), "").trim().toLowerCase(Locale.ROOT);
        String password = asString(data.get("password"), "");
        if (email.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        boolean defaultAdmin = email.equalsIgnoreCase(properties.getAdminEmail())
                && password.equals(properties.getAdminPassword());

        String orgId = properties.getDemoOrgId();
        String userId = properties.getDemoUserId();
        String role = "admin";

        if (!defaultAdmin) {
            List<Map<String, Object>> users = store.find(
                    "users",
                    Map.of("email", email, "status", "active"),
                    null,
                    false,
                    0,
                    1
            );
            if (users.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            Map<String, Object> user = users.get(0);
            String storedPassword = asString(user.get("password"), "");
            if (!storedPassword.isBlank() && !storedPassword.equals(password)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            orgId = asString(user.get("org_id"), orgId);
            userId = asString(user.get("id"), userId);
            role = asString(user.get("role"), "buyer");
        }

        String token = tokenService.createToken(userId, orgId, role, email);
        return Map.of(
                "access_token", token,
                "token_type", "bearer",
                "user", Map.of("id", userId, "org_id", orgId, "role", role, "email", email),
                "demo_mode", properties.isDemoMode()
        );
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me() {
        DemoContext ctx = requireContext();
        return Map.of(
                "id", ctx.userId(),
                "org_id", ctx.orgId(),
                "role", ctx.userRole(),
                "demo_mode", ctx.demo()
        );
    }

    @GetMapping("/demo/status")
    public Map<String, Object> demoStatus() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("demo_mode", properties.isDemoMode());
        out.put("demo_org_id", properties.isDemoMode() ? properties.getDemoOrgId() : null);
        out.put("demo_user_id", properties.isDemoMode() ? properties.getDemoUserId() : null);
        return out;
    }

    @PostMapping("/demo/reset")
    public Map<String, Object> resetDemo() {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        List<String> collections = List.of("projects", "suppliers", "normalized_products", "rfqs", "quotes", "price_points", "alert_rules", "alert_events");
        for (String coll : collections) {
            store.deleteByQuery(coll, Map.of("org_id", ctx.orgId()), false);
        }
        seedDemoData();
        return Map.of("message", "Demo data reset successfully");
    }

    // ==================== DASHBOARD ====================
    @GetMapping("/dashboard/stats")
    public Map<String, Object> dashboardStats() {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);

        long projectsCount = store.count("projects", query);
        long suppliersCount = store.count("suppliers", query);
        long rfqsCount = store.count("rfqs", query);
        long quotesCount = store.count("quotes", query);
        long activeAlerts = store.count("alert_events", Map.of("org_id", ctx.orgId(), "status", "new"));

        List<Map<String, Object>> recentRfqs = store.find("rfqs", query, "created_at", true, 0, 5);
        List<Map<String, Object>> recentQuotes = store.find("quotes", query, "created_at", true, 0, 5);
        List<Map<String, Object>> recentAlerts = store.find("alert_events", query, "triggered_at", true, 0, 5);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("projects_count", projectsCount);
        out.put("suppliers_count", suppliersCount);
        out.put("rfqs_count", rfqsCount);
        out.put("quotes_count", quotesCount);
        out.put("active_alerts", activeAlerts);
        out.put("recent_rfqs", recentRfqs);
        out.put("recent_quotes", recentQuotes);
        out.put("recent_alerts", recentAlerts);
        return out;
    }

    // ==================== PROJECTS ====================
    @GetMapping("/projects")
    public Map<String, Object> listProjects(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String status) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (status != null && !status.isBlank()) {
            query.put("status", status);
        }
        long total = store.count("projects", query);
        List<Map<String, Object>> items = store.find("projects", query, "created_at", true, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/projects/{projectId}")
    public Map<String, Object> getProject(@PathVariable String projectId) {
        DemoContext ctx = requireContext();
        return getOr404("projects", projectId, ctx.orgId(), "Project not found");
    }

    @PostMapping("/projects")
    public Map<String, Object> createProject(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.putIfAbsent("status", "active");
        doc.put("created_at", nowIso());
        doc.put("updated_at", nowIso());
        store.upsert("projects", doc);
        return sanitize(doc);
    }

    @PutMapping("/projects/{projectId}")
    public Map<String, Object> updateProject(@PathVariable String projectId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("projects", projectId, ctx.orgId(), "Project not found");
        Map<String, Object> updates = new LinkedHashMap<>(data);
        updates.remove("id");
        updates.remove("org_id");
        updates.put("updated_at", nowIso());
        store.updateByQuery("projects", Map.of("id", projectId), updates, true);
        return getOr404("projects", projectId, ctx.orgId(), "Project not found");
    }

    @DeleteMapping("/projects/{projectId}")
    public Map<String, Object> deleteProject(@PathVariable String projectId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("projects", projectId, ctx.orgId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return Map.of("message", "Project deleted");
    }

    // ==================== SUPPLIERS ====================
    @GetMapping("/suppliers")
    public Map<String, Object> listSuppliers(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String search) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (search != null && !search.isBlank()) {
            query.put("name", Map.of("$regex", search, "$options", "i"));
        }
        long total = store.count("suppliers", query);
        List<Map<String, Object>> items = store.find("suppliers", query, "name", false, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/suppliers/{supplierId}")
    public Map<String, Object> getSupplier(@PathVariable String supplierId) {
        DemoContext ctx = requireContext();
        return getOr404("suppliers", supplierId, ctx.orgId(), "Supplier not found");
    }

    @PostMapping("/suppliers")
    public Map<String, Object> createSupplier(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.put("created_at", nowIso());
        doc.put("updated_at", nowIso());
        doc.putIfAbsent("tags", new ArrayList<>());
        store.upsert("suppliers", doc);
        return sanitize(doc);
    }

    @PutMapping("/suppliers/{supplierId}")
    public Map<String, Object> updateSupplier(@PathVariable String supplierId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("suppliers", supplierId, ctx.orgId(), "Supplier not found");
        Map<String, Object> updates = new LinkedHashMap<>(data);
        updates.remove("id");
        updates.remove("org_id");
        updates.put("updated_at", nowIso());
        store.updateByQuery("suppliers", Map.of("id", supplierId), updates, true);
        return getOr404("suppliers", supplierId, ctx.orgId(), "Supplier not found");
    }

    @DeleteMapping("/suppliers/{supplierId}")
    public Map<String, Object> deleteSupplier(@PathVariable String supplierId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("suppliers", supplierId, ctx.orgId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found");
        }
        return Map.of("message", "Supplier deleted");
    }

    // ==================== CATALOG ====================
    @GetMapping("/catalog/products")
    public Map<String, Object> listProducts(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String search) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (category != null && !category.isBlank()) {
            query.put("category", category);
        }
        if (search != null && !search.isBlank()) {
            query.put("canonical_name", Map.of("$regex", search, "$options", "i"));
        }
        long total = store.count("normalized_products", query);
        List<Map<String, Object>> items = store.find("normalized_products", query, "canonical_name", false, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/catalog/products/{productId}")
    public Map<String, Object> getProduct(@PathVariable String productId) {
        DemoContext ctx = requireContext();
        return getOr404("normalized_products", productId, ctx.orgId(), "Product not found");
    }

    @PostMapping("/catalog/products")
    public Map<String, Object> createProduct(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.put("created_at", nowIso());
        doc.putIfAbsent("attributes", new LinkedHashMap<>());
        store.upsert("normalized_products", doc);
        return sanitize(doc);
    }

    @PutMapping("/catalog/products/{productId}")
    public Map<String, Object> updateProduct(@PathVariable String productId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("normalized_products", productId, ctx.orgId(), "Product not found");
        Map<String, Object> updates = new LinkedHashMap<>(data);
        updates.remove("id");
        updates.remove("org_id");
        store.updateByQuery("normalized_products", Map.of("id", productId), updates, true);
        return getOr404("normalized_products", productId, ctx.orgId(), "Product not found");
    }

    @DeleteMapping("/catalog/products/{productId}")
    public Map<String, Object> deleteProduct(@PathVariable String productId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("normalized_products", productId, ctx.orgId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return Map.of("message", "Product deleted");
    }

    @GetMapping("/catalog/categories")
    public Map<String, Object> listCategories() {
        DemoContext ctx = requireContext();
        List<String> categories = store.distinct("normalized_products", "category", Map.of("org_id", ctx.orgId()));
        return Map.of("categories", categories);
    }

    // ==================== RFQS ====================
    @GetMapping("/rfqs")
    public Map<String, Object> listRfqs(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String project_id) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (status != null && !status.isBlank()) {
            query.put("status", status);
        }
        if (project_id != null && !project_id.isBlank()) {
            query.put("project_id", project_id);
        }
        long total = store.count("rfqs", query);
        List<Map<String, Object>> items = store.find("rfqs", query, "created_at", true, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/rfqs/{rfqId}")
    public Map<String, Object> getRfq(@PathVariable String rfqId) {
        DemoContext ctx = requireContext();
        return getOr404("rfqs", rfqId, ctx.orgId(), "RFQ not found");
    }

    @PostMapping("/rfqs")
    public Map<String, Object> createRfq(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.put("created_by", ctx.userId());
        doc.putIfAbsent("status", "draft");
        doc.put("created_at", nowIso());
        doc.put("updated_at", nowIso());
        doc.putIfAbsent("supplier_ids", new ArrayList<>());

        List<Map<String, Object>> normalizedItems = new ArrayList<>();
        for (Object item : asList(doc.get("items"))) {
            Map<String, Object> itemDoc = asMap(item);
            itemDoc.putIfAbsent("id", uuid());
            itemDoc.putIfAbsent("specs", new LinkedHashMap<>());
            normalizedItems.add(itemDoc);
        }
        doc.put("items", normalizedItems);

        store.upsert("rfqs", doc);
        return sanitize(doc);
    }

    @PutMapping("/rfqs/{rfqId}")
    public Map<String, Object> updateRfq(@PathVariable String rfqId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("rfqs", rfqId, ctx.orgId(), "RFQ not found");

        Map<String, Object> updates = new LinkedHashMap<>();
        for (String key : List.of("title", "notes", "supplier_ids", "due_date")) {
            if (data.containsKey(key)) {
                updates.put(key, data.get(key));
            }
        }
        if (data.containsKey("items")) {
            List<Map<String, Object>> normalized = new ArrayList<>();
            for (Object item : asList(data.get("items"))) {
                Map<String, Object> itemDoc = asMap(item);
                itemDoc.putIfAbsent("id", uuid());
                itemDoc.putIfAbsent("specs", new LinkedHashMap<>());
                normalized.add(itemDoc);
            }
            updates.put("items", normalized);
        }
        updates.put("updated_at", nowIso());

        store.updateByQuery("rfqs", Map.of("id", rfqId), updates, true);
        return getOr404("rfqs", rfqId, ctx.orgId(), "RFQ not found");
    }

    @PostMapping("/rfqs/{rfqId}/send")
    public Map<String, Object> sendRfq(@PathVariable String rfqId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> rfq = getOr404("rfqs", rfqId, ctx.orgId(), "RFQ not found");
        if ("sent".equals(asString(rfq.get("status"), ""))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RFQ already sent");
        }

        List<String> supplierIds = asList(rfq.get("supplier_ids")).stream().map(String::valueOf).toList();
        if (supplierIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No suppliers selected");
        }

        List<Map<String, Object>> suppliers = store.find("suppliers", Map.of("org_id", ctx.orgId(), "id", Map.of("$in", supplierIds)), null, false, 0, 100);

        int sentCount = 0;
        for (Map<String, Object> supplier : suppliers) {
            String email = asString(supplier.get("contact_email"), "");
            if (!email.isBlank()) {
                sentCount++;
                log.info("RFQ {} email prepared for {} from {}", rfqId, email, properties.getSenderEmail());
            }
        }

        store.updateByQuery("rfqs", Map.of("id", rfqId), Map.of("status", "sent", "updated_at", nowIso()), true);
        return Map.of("message", "RFQ sent to " + sentCount + " suppliers", "status", "sent");
    }

    @DeleteMapping("/rfqs/{rfqId}")
    public Map<String, Object> deleteRfq(@PathVariable String rfqId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("rfqs", rfqId, ctx.orgId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RFQ not found");
        }
        return Map.of("message", "RFQ deleted");
    }

    // ==================== QUOTES ====================
    @GetMapping("/quotes")
    public Map<String, Object> listQuotes(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String supplier_id,
                                          @RequestParam(required = false) String rfq_id) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (status != null && !status.isBlank()) {
            query.put("status", status);
        }
        if (supplier_id != null && !supplier_id.isBlank()) {
            query.put("supplier_id", supplier_id);
        }
        if (rfq_id != null && !rfq_id.isBlank()) {
            query.put("rfq_id", rfq_id);
        }
        long total = store.count("quotes", query);
        List<Map<String, Object>> items = store.find("quotes", query, "created_at", true, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/quotes/{quoteId}")
    public Map<String, Object> getQuote(@PathVariable String quoteId) {
        DemoContext ctx = requireContext();
        return getOr404("quotes", quoteId, ctx.orgId(), "Quote not found");
    }

    @PostMapping("/quotes")
    public Map<String, Object> createQuote(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> quote = new LinkedHashMap<>(data);

        List<Map<String, Object>> items = new ArrayList<>();
        double totalAmount = 0.0;
        for (Object raw : asList(quote.get("items"))) {
            Map<String, Object> item = asMap(raw);
            item.putIfAbsent("id", uuid());
            double qty = asDouble(item.get("qty"), 0.0);
            double unitPrice = asDouble(item.get("unit_price"), 0.0);
            double totalPrice = qty * unitPrice;
            item.put("total_price", totalPrice);
            items.add(item);
            totalAmount += totalPrice;
        }

        quote.put("id", uuid());
        quote.put("org_id", ctx.orgId());
        quote.putIfAbsent("status", "received");
        quote.putIfAbsent("currency", "USD");
        quote.putIfAbsent("attachments", new ArrayList<>());
        quote.put("items", items);
        quote.put("total_amount", totalAmount);
        quote.put("received_at", nowIso());
        quote.put("created_at", nowIso());
        quote.put("updated_at", nowIso());

        store.upsert("quotes", quote);

        for (Map<String, Object> item : items) {
            String productId = asString(item.get("normalized_product_id"), "");
            if (!productId.isBlank() && !"null".equalsIgnoreCase(productId)) {
                createPricePointFromQuoteItem(ctx, quote, item, productId);
                evaluateAlertsForProduct(ctx.orgId(), productId, asDouble(item.get("unit_price"), 0.0));
            }
        }

        return sanitize(quote);
    }

    @PutMapping("/quotes/{quoteId}")
    public Map<String, Object> updateQuote(@PathVariable String quoteId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("quotes", quoteId, ctx.orgId(), "Quote not found");

        Map<String, Object> updates = new LinkedHashMap<>();
        for (String key : List.of("status", "payment_terms", "delivery_terms")) {
            if (data.containsKey(key)) {
                updates.put(key, data.get(key));
            }
        }

        if (data.containsKey("items")) {
            List<Map<String, Object>> items = new ArrayList<>();
            double totalAmount = 0.0;
            for (Object raw : asList(data.get("items"))) {
                Map<String, Object> item = asMap(raw);
                item.putIfAbsent("id", uuid());
                double qty = asDouble(item.get("qty"), 0.0);
                double unitPrice = asDouble(item.get("unit_price"), 0.0);
                item.put("total_price", qty * unitPrice);
                totalAmount += qty * unitPrice;
                items.add(item);
            }
            updates.put("items", items);
            updates.put("total_amount", totalAmount);
        }

        updates.put("updated_at", nowIso());
        store.updateByQuery("quotes", Map.of("id", quoteId), updates, true);
        return getOr404("quotes", quoteId, ctx.orgId(), "Quote not found");
    }

    @PostMapping("/quotes/{quoteId}/map-item/{itemId}")
    public Map<String, Object> mapQuoteItem(@PathVariable String quoteId,
                                            @PathVariable String itemId,
                                            @RequestParam("product_id") String productId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> quote = getOr404("quotes", quoteId, ctx.orgId(), "Quote not found");
        getOr404("normalized_products", productId, ctx.orgId(), "Product not found");

        List<Map<String, Object>> items = new ArrayList<>();
        boolean found = false;
        for (Object raw : asList(quote.get("items"))) {
            Map<String, Object> item = asMap(raw);
            if (itemId.equals(asString(item.get("id"), ""))) {
                item.put("normalized_product_id", productId);
                found = true;
                createPricePointFromQuoteItem(ctx, quote, item, productId);
                evaluateAlertsForProduct(ctx.orgId(), productId, asDouble(item.get("unit_price"), 0.0));
            }
            items.add(item);
        }

        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }

        store.updateByQuery("quotes", Map.of("id", quoteId), Map.of("items", items), true);
        return Map.of("message", "Item mapped successfully");
    }

    @DeleteMapping("/quotes/{quoteId}")
    public Map<String, Object> deleteQuote(@PathVariable String quoteId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("quotes", quoteId, ctx.orgId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found");
        }
        return Map.of("message", "Quote deleted");
    }

    private void createPricePointFromQuoteItem(DemoContext ctx, Map<String, Object> quote, Map<String, Object> item, String productId) {
        Map<String, Object> pp = new LinkedHashMap<>();
        pp.put("id", uuid());
        pp.put("org_id", ctx.orgId());
        pp.put("normalized_product_id", productId);
        pp.put("source_type", "quote");
        pp.put("source_id", quote.get("id"));
        pp.put("observed_at", nowIso());
        pp.put("currency", asString(quote.get("currency"), "USD"));
        pp.put("unit_price_normalized", asDouble(item.get("unit_price"), 0.0));
        pp.put("uom_normalized", asString(item.get("uom"), ""));
        pp.put("supplier_id", quote.get("supplier_id"));
        pp.put("meta", new LinkedHashMap<>());
        store.upsert("price_points", pp);
    }

    // ==================== PRICE HISTORY ====================
    @GetMapping("/price-history")
    public Map<String, Object> getPriceHistory(@RequestParam(required = false) String product_id,
                                               @RequestParam(required = false) String supplier_id,
                                               @RequestParam(defaultValue = "90") int days) {
        DemoContext ctx = requireContext();
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("org_id", ctx.orgId());
        query.put("observed_at", Map.of("$gte", cutoff.toString()));
        if (product_id != null && !product_id.isBlank()) {
            query.put("normalized_product_id", product_id);
        }
        if (supplier_id != null && !supplier_id.isBlank()) {
            query.put("supplier_id", supplier_id);
        }

        List<Map<String, Object>> pricePoints = store.find("price_points", query, "observed_at", false, 0, 1000);
        return Map.of("price_points", pricePoints);
    }

    @GetMapping("/price-history/product/{productId}")
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

        Set<String> supplierIds = pricePoints.stream()
                .map(d -> asString(d.get("supplier_id"), ""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        Map<String, String> supplierMap = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            List<Map<String, Object>> suppliers = store.find(
                    "suppliers",
                    Map.of("org_id", ctx.orgId(), "id", Map.of("$in", new ArrayList<>(supplierIds))),
                    null,
                    false,
                    0,
                    100
            );
            for (Map<String, Object> s : suppliers) {
                supplierMap.put(asString(s.get("id"), ""), asString(s.get("name"), "Unknown"));
            }
        }

        for (Map<String, Object> pp : pricePoints) {
            String sid = asString(pp.get("supplier_id"), "");
            if (!sid.isBlank()) {
                pp.put("supplier_name", supplierMap.getOrDefault(sid, "Unknown"));
            }
        }

        return Map.of("product", product, "price_points", pricePoints);
    }

    // ==================== ALERTS ====================
    @GetMapping("/alerts/rules")
    public Map<String, Object> listAlertRules() {
        DemoContext ctx = requireContext();
        List<Map<String, Object>> rules = store.find("alert_rules", Map.of("org_id", ctx.orgId()), null, false, 0, 100);
        return Map.of("rules", rules);
    }

    @PostMapping("/alerts/rules")
    public Map<String, Object> createAlertRule(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.putIfAbsent("is_active", true);
        doc.putIfAbsent("params", new LinkedHashMap<>());
        doc.put("created_at", nowIso());
        store.upsert("alert_rules", doc);
        return sanitize(doc);
    }

    @PutMapping("/alerts/rules/{ruleId}")
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

    @DeleteMapping("/alerts/rules/{ruleId}")
    public Map<String, Object> deleteAlertRule(@PathVariable String ruleId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("alert_rules", ruleId, ctx.orgId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found");
        }
        return Map.of("message", "Rule deleted");
    }

    @GetMapping("/alerts/events")
    public Map<String, Object> listAlertEvents(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String status) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("org_id", ctx.orgId());
        if (status != null && !status.isBlank()) {
            query.put("status", status);
        }

        long total = store.count("alert_events", query);
        List<Map<String, Object>> events = store.find("alert_events", query, "triggered_at", true, (page - 1) * pageSize, pageSize);

        Set<String> productIds = events.stream()
                .map(e -> asString(e.get("normalized_product_id"), ""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        Map<String, String> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Map<String, Object>> products = store.find(
                    "normalized_products",
                    Map.of("org_id", ctx.orgId(), "id", Map.of("$in", new ArrayList<>(productIds))),
                    null,
                    false,
                    0,
                    100
            );
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

    @PutMapping("/alerts/events/{eventId}")
    public Map<String, Object> updateAlertEvent(@PathVariable String eventId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        Map<String, Object> event = getOr404("alert_events", eventId, ctx.orgId(), "Event not found");
        event.put("status", data.get("status"));
        store.upsert("alert_events", event);
        return Map.of("message", "Event updated");
    }

    private void evaluateAlertsForProduct(String orgId, String productId, double newPrice) {
        List<Map<String, Object>> rules = store.find("alert_rules", Map.of("org_id", orgId, "is_active", true), null, false, 0, 100);
        for (Map<String, Object> rule : rules) {
            Map<String, Object> params = asMap(rule.get("params"));
            double thresholdPercent = asDouble(params.get("threshold_percent"), 10.0);
            int compareLastN = asInt(params.get("compare_last_n"), 3);

            List<Map<String, Object>> recentPrices = store.find(
                    "price_points",
                    Map.of("org_id", orgId, "normalized_product_id", productId),
                    "observed_at",
                    true,
                    0,
                    compareLastN + 1
            );

            if (recentPrices.size() < 2) {
                continue;
            }

            double lastPrice = asDouble(recentPrices.get(1).get("unit_price_normalized"), 0.0);
            if (lastPrice <= 0) {
                continue;
            }

            double changePercent = ((newPrice - lastPrice) / lastPrice) * 100.0;
            if (Math.abs(changePercent) >= thresholdPercent) {
                String severity = Math.abs(changePercent) >= thresholdPercent * 2 ? "high" : "medium";
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("new_price", newPrice);
                payload.put("last_price", lastPrice);
                payload.put("change_percent", Math.round(changePercent * 100.0) / 100.0);
                payload.put("rule_name", asString(rule.get("name"), ""));

                Map<String, Object> event = new LinkedHashMap<>();
                event.put("id", uuid());
                event.put("org_id", orgId);
                event.put("rule_id", rule.get("id"));
                event.put("normalized_product_id", productId);
                event.put("triggered_at", nowIso());
                event.put("severity", severity);
                event.put("payload", payload);
                event.put("status", "new");
                store.upsert("alert_events", event);
            }
        }
    }

    // ==================== QUOTE COMPARISON ====================
    @GetMapping("/quotes/compare")
    public Map<String, Object> compareQuotes(@RequestParam("quote_ids") String quoteIds) {
        DemoContext ctx = requireContext();
        List<String> ids = Arrays.stream(quoteIds.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();

        List<Map<String, Object>> quotes = store.find("quotes", Map.of("id", Map.of("$in", ids), "org_id", ctx.orgId()), null, false, 0, 10);
        if (quotes.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 quotes to compare");
        }

        Set<String> supplierIds = quotes.stream().map(q -> asString(q.get("supplier_id"), "")).filter(s -> !s.isBlank()).collect(Collectors.toSet());
        Map<String, String> supplierMap = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            List<Map<String, Object>> suppliers = store.find(
                    "suppliers",
                    Map.of("org_id", ctx.orgId(), "id", Map.of("$in", new ArrayList<>(supplierIds))),
                    null,
                    false,
                    0,
                    100
            );
            for (Map<String, Object> s : suppliers) {
                supplierMap.put(asString(s.get("id"), ""), asString(s.get("name"), "Unknown"));
            }
        }

        for (Map<String, Object> quote : quotes) {
            String sid = asString(quote.get("supplier_id"), "");
            quote.put("supplier_name", supplierMap.getOrDefault(sid, "Unknown"));
        }

        return Map.of("quotes", quotes);
    }

    // ==================== HEALTH ====================
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "healthy", "timestamp", nowIso());
    }

    @GetMapping("/ready")
    public Map<String, Object> ready() {
        try {
            store.count("organizations", Map.of());
            return Map.of("status", "ready", "database", "connected");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Database not ready: " + e.getMessage());
        }
    }

    // ==================== DEMO SEED ====================
    @PostMapping("/__seed")
    public Map<String, Object> seedEndpoint() {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        seedDemoData();
        return Map.of("message", "seeded");
    }

    public void seedDemoData() {
        if (!properties.isDemoMode()) {
            return;
        }

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
                    "status", "active",
                    "created_at", nowIso()
            )));
        }

        if (store.count("projects", Map.of("org_id", orgId)) == 0) {
            List<Map<String, Object>> demoProjects = List.of(
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Downtown Office Tower", "location", "New York, NY", "status", "active", "description", "45-story commercial building", "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Riverside Residential", "location", "Chicago, IL", "status", "active", "description", "Mixed-use residential complex", "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Industrial Park Phase 2", "location", "Houston, TX", "status", "on_hold", "description", "Warehouse and logistics facility", "created_at", nowIso(), "updated_at", nowIso()))
            );
            demoProjects.forEach(d -> store.upsert("projects", d));
        }

        if (store.count("suppliers", Map.of("org_id", orgId)) == 0) {
            List<Map<String, Object>> demoSuppliers = List.of(
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Steel Solutions Inc.", "contact_email", "sales@steelsolutions.com", "phone", "+1-555-0101", "tags", List.of("steel", "structural"), "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Concrete Masters LLC", "contact_email", "orders@concretemasters.com", "phone", "+1-555-0102", "tags", List.of("concrete", "cement"), "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Electrical Supplies Co.", "contact_email", "info@electricalsupplies.com", "phone", "+1-555-0103", "tags", List.of("electrical", "wiring"), "created_at", nowIso(), "updated_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "name", "Plumbing Pro Distributors", "contact_email", "orders@plumbingpro.com", "phone", "+1-555-0104", "tags", List.of("plumbing", "pipes"), "created_at", nowIso(), "updated_at", nowIso()))
            );
            demoSuppliers.forEach(d -> store.upsert("suppliers", d));
        }

        if (store.count("normalized_products", Map.of("org_id", orgId)) == 0) {
            List<Map<String, Object>> demoProducts = List.of(
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "Rebar #4 Grade 60", "category", "Steel", "base_uom", "ton", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "Ready-Mix Concrete 4000 PSI", "category", "Concrete", "base_uom", "cubic_yard", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "12/2 Romex Wire", "category", "Electrical", "base_uom", "ft", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "3/4\" Copper Pipe Type L", "category", "Plumbing", "base_uom", "ft", "attributes", new LinkedHashMap<>(), "created_at", nowIso())),
                    new LinkedHashMap<>(Map.of("id", uuid(), "org_id", orgId, "canonical_name", "Portland Cement Type I/II", "category", "Concrete", "base_uom", "bag", "attributes", new LinkedHashMap<>(), "created_at", nowIso()))
            );
            demoProducts.forEach(d -> store.upsert("normalized_products", d));
        }
    }
}
