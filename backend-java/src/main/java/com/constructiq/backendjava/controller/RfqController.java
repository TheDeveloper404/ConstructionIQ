package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rfqs")
public class RfqController extends ControllerBase {

    private static final Logger log = LoggerFactory.getLogger(RfqController.class);

    public RfqController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping
    public Map<String, Object> listRfqs(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String project_id) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (status != null && !status.isBlank()) query.put("status", status);
        if (project_id != null && !project_id.isBlank()) query.put("project_id", project_id);
        long total = store.count("rfqs", query);
        List<Map<String, Object>> items = store.find("rfqs", query, "created_at", true, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/{rfqId}")
    public Map<String, Object> getRfq(@PathVariable String rfqId) {
        DemoContext ctx = requireContext();
        return getOr404("rfqs", rfqId, ctx.orgId(), "RFQ not found");
    }

    @PostMapping
    public Map<String, Object> createRfq(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        requireNonBlank(data, "title", "RFQ title is required");
        requireNonBlank(data, "project_id", "RFQ project_id is required");
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.put("created_by", ctx.userId());
        doc.putIfAbsent("status", "draft");
        doc.put("created_at", nowIso());
        doc.put("updated_at", nowIso());
        doc.putIfAbsent("supplier_ids", new ArrayList<>());

        List<Map<String, Object>> normalizedItems = new ArrayList<>();
        for (Object item : requireNonEmptyList(doc, "items", "RFQ must contain at least one item")) {
            Map<String, Object> itemDoc = asMap(item);
            requireNonBlank(itemDoc, "raw_text", "RFQ item raw_text is required");
            itemDoc.putIfAbsent("id", uuid());
            itemDoc.putIfAbsent("specs", new LinkedHashMap<>());
            normalizedItems.add(itemDoc);
        }
        doc.put("items", normalizedItems);
        store.upsert("rfqs", doc);
        return sanitize(doc);
    }

    @PutMapping("/{rfqId}")
    public Map<String, Object> updateRfq(@PathVariable String rfqId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("rfqs", rfqId, ctx.orgId(), "RFQ not found");

        Map<String, Object> updates = new LinkedHashMap<>();
        for (String key : List.of("title", "notes", "supplier_ids", "due_date")) {
            if (data.containsKey(key)) updates.put(key, data.get(key));
        }
        if (data.containsKey("items")) {
            List<Map<String, Object>> normalized = new ArrayList<>();
            for (Object item : requireNonEmptyList(data, "items", "RFQ must contain at least one item")) {
                Map<String, Object> itemDoc = asMap(item);
                requireNonBlank(itemDoc, "raw_text", "RFQ item raw_text is required");
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

    @PostMapping("/{rfqId}/send")
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
        List<Map<String, Object>> suppliers = store.find(
                "suppliers",
                Map.of("org_id", ctx.orgId(), "id", Map.of("$in", supplierIds)),
                null, false, 0, 100);

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

    @DeleteMapping("/{rfqId}")
    public Map<String, Object> deleteRfq(@PathVariable String rfqId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("rfqs", rfqId, ctx.orgId());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RFQ not found");
        return Map.of("message", "RFQ deleted");
    }
}
