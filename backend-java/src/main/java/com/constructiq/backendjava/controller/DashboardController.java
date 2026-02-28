package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.model.DemoContext;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends ControllerBase {

    public DashboardController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping("/stats")
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
}
