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
@RequestMapping("/api/projects")
public class ProjectController extends ControllerBase {

    public ProjectController(SqlDocumentStore store, ConstructIQProperties properties) {
        super(store, properties);
    }

    @GetMapping
    public Map<String, Object> listProjects(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String status) {
        DemoContext ctx = requireContext();
        Map<String, Object> query = baseOrgQuery(ctx);
        if (status != null && !status.isBlank()) query.put("status", status);
        long total = store.count("projects", query);
        List<Map<String, Object>> items = store.find("projects", query, "created_at", true, (page - 1) * pageSize, pageSize);
        return paginate(items, total, page, pageSize);
    }

    @GetMapping("/{projectId}")
    public Map<String, Object> getProject(@PathVariable String projectId) {
        DemoContext ctx = requireContext();
        return getOr404("projects", projectId, ctx.orgId(), "Project not found");
    }

    @PostMapping
    public Map<String, Object> createProject(@RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        requireNonBlank(data, "name", "Project name is required");
        Map<String, Object> doc = new LinkedHashMap<>(data);
        doc.put("id", uuid());
        doc.put("org_id", ctx.orgId());
        doc.putIfAbsent("status", "active");
        doc.put("created_at", nowIso());
        doc.put("updated_at", nowIso());
        store.upsert("projects", doc);
        return sanitize(doc);
    }

    @PutMapping("/{projectId}")
    public Map<String, Object> updateProject(@PathVariable String projectId, @RequestBody Map<String, Object> data) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        getOr404("projects", projectId, ctx.orgId(), "Project not found");
        if (data.containsKey("name")) requireNonBlank(data, "name", "Project name is required");
        Map<String, Object> updates = new LinkedHashMap<>(data);
        updates.remove("id");
        updates.remove("org_id");
        updates.put("updated_at", nowIso());
        store.updateByQuery("projects", Map.of("id", projectId), updates, true);
        return getOr404("projects", projectId, ctx.orgId(), "Project not found");
    }

    @DeleteMapping("/{projectId}")
    public Map<String, Object> deleteProject(@PathVariable String projectId) {
        DemoContext ctx = requireContext();
        requireAdmin(ctx);
        long deleted = store.deleteOne("projects", projectId, ctx.orgId());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        return Map.of("message", "Project deleted");
    }
}
