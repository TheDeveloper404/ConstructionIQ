package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.service.AlertService;
import com.constructiq.backendjava.service.PricePointService;
import com.constructiq.backendjava.service.QuoteService;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController extends ControllerBase {

    private final QuoteService quoteService;
    private final PricePointService pricePointService;
    private final AlertService alertService;

    public QuoteController(SqlDocumentStore store,
                           ConstructIQProperties properties,
                           QuoteService quoteService,
                           PricePointService pricePointService,
                           AlertService alertService) {
        super(store, properties);
        this.quoteService = quoteService;
        this.pricePointService = pricePointService;
        this.alertService = alertService;
    }

    @GetMapping
    public Map<String, Object> listQuotes(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String supplier_id,
                                          @RequestParam(required = false) String rfq_id) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (status != null && !status.isBlank()) query.put("status", status);
        if (supplier_id != null && !supplier_id.isBlank()) query.put("supplier_id", supplier_id);
        if (rfq_id != null && !rfq_id.isBlank()) query.put("rfq_id", rfq_id);
        long total = store.count("quotes", query);
        List<Map<String, Object>> items = store.find("quotes", query, "created_at", true, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/compare")
    public Map<String, Object> compareQuotes(@RequestParam("quote_ids") String quoteIds) {
        DemoContext ctx = requireContext();
        List<String> ids = Arrays.stream(quoteIds.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
        List<Map<String, Object>> quotes = store.find(
                "quotes", Map.of("id", Map.of("$in", ids), "org_id", ctx.orgId()),
                null, false, 0, 10);
        if (quotes.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 quotes to compare");
        }
        Set<String> supplierIds = new HashSet<>();
        for (Map<String, Object> q : quotes) {
            String sid = asString(q.get("supplier_id"), "");
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
        for (Map<String, Object> q : quotes) {
            String sid = asString(q.get("supplier_id"), "");
            q.put("supplier_name", supplierMap.getOrDefault(sid, "Unknown"));
        }
        return Map.of("quotes", quotes);
    }

    @GetMapping("/{quoteId}")
    public Map<String, Object> getQuote(@PathVariable String quoteId) {
        DemoContext ctx = requireContext();
        return getOr404("quotes", quoteId, ctx.orgId(), "Quote not found");
    }

    @PostMapping
    public Map<String, Object> createQuote(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        return quoteService.buildAndPersist(ctx, data);
    }

    @PutMapping("/{quoteId}")
    public Map<String, Object> updateQuote(@PathVariable String quoteId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("quotes", quoteId, ctx.orgId(), "Quote not found");

        Map<String, Object> updates = new LinkedHashMap<>();
        for (String key : List.of("status", "payment_terms", "delivery_terms")) {
            if (data.containsKey(key)) updates.put(key, data.get(key));
        }
        if (data.containsKey("items")) {
            List<Map<String, Object>> items = new ArrayList<>();
            double totalAmount = 0.0;
            for (Object raw : requireNonEmptyList(data, "items", "Quote must contain at least one item")) {
                Map<String, Object> item = asMap(raw);
                requireNonBlank(item, "raw_line_text", "Quote item raw_line_text is required");
                item.putIfAbsent("id", uuid());
                double qty = asDouble(item.get("qty"), 0.0);
                double unitPrice = asDouble(item.get("unit_price"), 0.0);
                if (qty <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote item qty must be greater than 0");
                if (unitPrice < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote item unit_price must be >= 0");
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

    @PostMapping("/{quoteId}/map-item/{itemId}")
    public Map<String, Object> mapQuoteItem(@PathVariable String quoteId,
                                            @PathVariable String itemId,
                                            @RequestParam("product_id") String productId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        if (productId == null || productId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "product_id is required");
        }
        Map<String, Object> quote = getOr404("quotes", quoteId, ctx.orgId(), "Quote not found");
        getOr404("normalized_products", productId, ctx.orgId(), "Product not found");

        List<Map<String, Object>> items = new ArrayList<>();
        boolean found = false;
        for (Object raw : asList(quote.get("items"))) {
            Map<String, Object> item = asMap(raw);
            if (itemId.equals(asString(item.get("id"), ""))) {
                item.put("normalized_product_id", productId);
                found = true;
                pricePointService.createFromQuoteItem(ctx, quote, item, productId);
                alertService.evaluateForProduct(ctx.orgId(), productId, asDouble(item.get("unit_price"), 0.0));
            }
            items.add(item);
        }
        if (!found) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        store.updateByQuery("quotes", Map.of("id", quoteId), Map.of("items", items), true);
        return Map.of("message", "Item mapped successfully");
    }

    @DeleteMapping("/{quoteId}")
    public Map<String, Object> deleteQuote(@PathVariable String quoteId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("quotes", quoteId, ctx.orgId());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found");
        return Map.of("message", "Quote deleted");
    }
}
