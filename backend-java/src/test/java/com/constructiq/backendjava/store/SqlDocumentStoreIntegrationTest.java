package com.constructiq.backendjava.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
class SqlDocumentStoreIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private SqlDocumentStore store;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM documents WHERE org_id IN ('org-1','org-2','org-count','org-del','org-upd','org-sort')");
    }

    @Test
    void upsert_and_findOne_roundtrip() {
        Map<String, Object> doc = Map.of(
                "id", "p1",
                "org_id", "org-1",
                "name", "Test Project",
                "status", "active"
        );

        store.upsert("projects", doc);
        Optional<Map<String, Object>> result = store.findOne("projects", "p1", "org-1");

        assertTrue(result.isPresent());
        assertEquals("Test Project", result.get().get("name"));
        assertEquals("active", result.get().get("status"));
    }

    @Test
    void find_filtersByOrgId() {
        store.upsert("projects", Map.of("id", "p1", "org_id", "org-1", "name", "Alpha", "status", "active"));
        store.upsert("projects", Map.of("id", "p2", "org_id", "org-2", "name", "Beta", "status", "active"));

        List<Map<String, Object>> results = store.find("projects", Map.of("org_id", "org-1"), null, false, 0, 10);

        assertEquals(1, results.size());
        assertEquals("Alpha", results.get(0).get("name"));
    }

    @Test
    void count_returnsCorrectCount() {
        store.upsert("projects", Map.of("id", "c1", "org_id", "org-count", "status", "active"));
        store.upsert("projects", Map.of("id", "c2", "org_id", "org-count", "status", "active"));
        store.upsert("projects", Map.of("id", "c3", "org_id", "org-count", "status", "on_hold"));

        long activeCount = store.count("projects", Map.of("org_id", "org-count", "status", "active"));
        long totalCount = store.count("projects", Map.of("org_id", "org-count"));

        assertEquals(2, activeCount);
        assertEquals(3, totalCount);
    }

    @Test
    void delete_removesDocument() {
        store.upsert("projects", Map.of("id", "del-1", "org_id", "org-del", "name", "ToDelete"));

        store.deleteOne("projects", "del-1", "org-del");

        Optional<Map<String, Object>> result = store.findOne("projects", "del-1", "org-del");
        assertFalse(result.isPresent());
    }

    @Test
    void updateByQuery_updatesMatchingDocuments() {
        store.upsert("rfqs", Map.of("id", "r1", "org_id", "org-upd", "status", "draft", "title", "RFQ 1"));
        store.upsert("rfqs", Map.of("id", "r2", "org_id", "org-upd", "status", "draft", "title", "RFQ 2"));
        store.upsert("rfqs", Map.of("id", "r3", "org_id", "org-upd", "status", "sent", "title", "RFQ 3"));

        long updated = store.updateByQuery("rfqs", Map.of("org_id", "org-upd", "status", "draft"),
                Map.of("status", "sent"), false);

        assertEquals(2, updated);
        long draftCount = store.count("rfqs", Map.of("org_id", "org-upd", "status", "draft"));
        assertEquals(0, draftCount);
    }

    @Test
    void find_withSortAndPagination() {
        store.upsert("projects", Map.of("id", "s1", "org_id", "org-sort", "name", "Charlie", "created_at", "2024-03-01"));
        store.upsert("projects", Map.of("id", "s2", "org_id", "org-sort", "name", "Alpha", "created_at", "2024-01-01"));
        store.upsert("projects", Map.of("id", "s3", "org_id", "org-sort", "name", "Beta", "created_at", "2024-02-01"));

        List<Map<String, Object>> page1 = store.find("projects", Map.of("org_id", "org-sort"),
                "created_at", false, 0, 2);
        List<Map<String, Object>> page2 = store.find("projects", Map.of("org_id", "org-sort"),
                "created_at", false, 2, 2);

        assertEquals(2, page1.size());
        assertEquals(1, page2.size());
        assertEquals("Alpha", page1.get(0).get("name"));
        assertEquals("Beta", page1.get(1).get("name"));
        assertEquals("Charlie", page2.get(0).get("name"));
    }

    @Test
    void upsert_updatesExistingDocument() {
        store.upsert("projects", Map.of("id", "upd-1", "org_id", "org-u", "name", "Original", "status", "active"));
        store.upsert("projects", Map.of("id", "upd-1", "org_id", "org-u", "name", "Updated", "status", "completed"));

        Optional<Map<String, Object>> result = store.findOne("projects", "upd-1", "org-u");

        assertTrue(result.isPresent());
        assertEquals("Updated", result.get().get("name"));
        assertEquals("completed", result.get().get("status"));
    }
}
