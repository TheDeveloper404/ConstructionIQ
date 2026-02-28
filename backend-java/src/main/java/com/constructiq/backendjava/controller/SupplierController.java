package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController extends ControllerBase {

    public SupplierController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping
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

    @GetMapping("/{supplierId}")
    public Map<String, Object> getSupplier(@PathVariable String supplierId) {
        DemoContext ctx = requireContext();
        return getOr404("suppliers", supplierId, ctx.orgId(), "Supplier not found");
    }

    @PostMapping
    public Map<String, Object> createSupplier(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        requireNonBlank(data, "name", "Supplier name is required");
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.put("created_at", nowIso());
        doc.put("updated_at", nowIso());
        doc.putIfAbsent("tags", new ArrayList<>());
        store.upsert("suppliers", doc);
        return sanitize(doc);
    }

    @PutMapping("/{supplierId}")
    public Map<String, Object> updateSupplier(@PathVariable String supplierId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("suppliers", supplierId, ctx.orgId(), "Supplier not found");
        if (data.containsKey("name")) requireNonBlank(data, "name", "Supplier name is required");
        Map<String, Object> updates = new LinkedHashMap<>(data);
        updates.remove("id");
        updates.remove("org_id");
        updates.put("updated_at", nowIso());
        store.updateByQuery("suppliers", Map.of("id", supplierId), updates, true);
        return getOr404("suppliers", supplierId, ctx.orgId(), "Supplier not found");
    }

    @DeleteMapping("/{supplierId}")
    public Map<String, Object> deleteSupplier(@PathVariable String supplierId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("suppliers", supplierId, ctx.orgId());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found");
        return Map.of("message", "Supplier deleted");
    }
}
