package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController extends ControllerBase {

    public CatalogController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping("/products")
    public Map<String, Object> listProducts(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String search) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (category != null && !category.isBlank()) query.put("category", category);
        if (search != null && !search.isBlank()) query.put("canonical_name", Map.of("$regex", search, "$options", "i"));
        long total = store.count("normalized_products", query);
        List<Map<String, Object>> items = store.find("normalized_products", query, "canonical_name", false, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/products/{productId}")
    public Map<String, Object> getProduct(@PathVariable String productId) {
        DemoContext ctx = requireContext();
        return getOr404("normalized_products", productId, ctx.orgId(), "Product not found");
    }

    @PostMapping("/products")
    public Map<String, Object> createProduct(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        requireNonBlank(data, "canonical_name", "Product canonical_name is required");
        requireNonBlank(data, "category", "Product category is required");
        requireNonBlank(data, "base_uom", "Product base_uom is required");
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.put("created_at", nowIso());
        doc.putIfAbsent("attributes", new LinkedHashMap<>());
        store.upsert("normalized_products", doc);
        return sanitize(doc);
    }

    @PutMapping("/products/{productId}")
    public Map<String, Object> updateProduct(@PathVariable String productId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("normalized_products", productId, ctx.orgId(), "Product not found");
        if (data.containsKey("canonical_name")) requireNonBlank(data, "canonical_name", "Product canonical_name is required");
        if (data.containsKey("category")) requireNonBlank(data, "category", "Product category is required");
        if (data.containsKey("base_uom")) requireNonBlank(data, "base_uom", "Product base_uom is required");
        Map<String, Object> updates = new LinkedHashMap<>(data);
        updates.remove("id");
        updates.remove("org_id");
        store.updateByQuery("normalized_products", Map.of("id", productId), updates, true);
        return getOr404("normalized_products", productId, ctx.orgId(), "Product not found");
    }

    @DeleteMapping("/products/{productId}")
    public Map<String, Object> deleteProduct(@PathVariable String productId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("normalized_products", productId, ctx.orgId());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        return Map.of("message", "Product deleted");
    }

    @GetMapping("/categories")
    public Map<String, Object> listCategories() {
        DemoContext ctx = requireContext();
        List<String> categories = store.distinct("normalized_products", "category", Map.of("org_id", ctx.orgId()));
        return Map.of("categories", categories);
    }
}
