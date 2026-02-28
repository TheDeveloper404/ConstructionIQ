package com.constructiq.backendjava.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SqlDocumentStoreTest {

    @Mock
    private JdbcTemplate jdbc;

    private SqlDocumentStore store;

    @BeforeEach
    void setUp() {
        store = new SqlDocumentStore(jdbc, new ObjectMapper());
    }

    @Test
    void findOne_existingDoc_returnsParsedMap() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("projects"), eq("doc-1")))
                .thenReturn(List.of(Map.of("id", "doc-1", "name", "Test")));

        Optional<Map<String, Object>> result = store.findOne("projects", "doc-1");
        assertTrue(result.isPresent());
        assertEquals("doc-1", result.get().get("id"));
    }

    @Test
    void findOne_nonExistentDoc_returnsEmpty() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("projects"), eq("missing")))
                .thenReturn(List.of());

        assertTrue(store.findOne("projects", "missing").isEmpty());
    }

    @Test
    void findOne_withOrgId_filtersCorrectly() {
        Map<String, Object> doc = new LinkedHashMap<>(Map.of("id", "doc-1", "org_id", "org-A"));
        when(jdbc.query(anyString(), any(RowMapper.class), eq("projects"), eq("doc-1")))
                .thenReturn(List.of(doc));

        assertTrue(store.findOne("projects", "doc-1", "org-A").isPresent());
        assertTrue(store.findOne("projects", "doc-1", "org-B").isEmpty());
    }

    @Test
    void upsert_callsJdbcUpdate() {
        Map<String, Object> doc = new LinkedHashMap<>(Map.of("id", "doc-1", "org_id", "org-1", "name", "Test"));
        store.upsert("projects", doc);
        verify(jdbc, times(1)).update(anyString(), eq("projects"), eq("doc-1"), eq("org-1"), anyString());
    }

    @Test
    void deleteOne_callsJdbcUpdate() {
        when(jdbc.update(anyString(), eq("projects"), eq("doc-1"), eq("org-1"))).thenReturn(1);
        long deleted = store.deleteOne("projects", "doc-1", "org-1");
        assertEquals(1, deleted);
    }

    @Test
    void count_simpleQuery_usesSqlCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);
        long count = store.count("projects", Map.of("org_id", "org-1"));
        assertEquals(5L, count);
        verify(jdbc, times(1)).queryForObject(contains("COUNT(*)"), eq(Long.class), any(Object[].class));
        verify(jdbc, never()).query(anyString(), any(RowMapper.class), eq("projects"));
    }

    @Test
    void find_simpleQuery_usesSqlWithLimit() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(new LinkedHashMap<>(Map.of("id", "r1", "org_id", "org-1"))));

        List<Map<String, Object>> results = store.find("projects", Map.of("org_id", "org-1"), "created_at", true, 0, 10);

        assertEquals(1, results.size());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        assertTrue(sqlCaptor.getValue().contains("LIMIT 10"));
        assertTrue(sqlCaptor.getValue().contains("ORDER BY"));
    }

    @Test
    void find_complexQuery_fallsBackToInMemoryFilter() {
        Map<String, Object> doc1 = new LinkedHashMap<>(Map.of("id", "r1", "org_id", "org-1", "name", "Alpha"));
        Map<String, Object> doc2 = new LinkedHashMap<>(Map.of("id", "r2", "org_id", "org-1", "name", "Beta"));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(doc1, doc2));

        List<Map<String, Object>> results = store.find(
                "projects",
                Map.of("org_id", "org-1", "name", Map.of("$regex", "alpha")),
                null, false, 0, 10);

        assertEquals(1, results.size());
        assertEquals("r1", results.get(0).get("id"));
    }

    @Test
    void updateByQuery_mergesUpdatesCorrectly() {
        Map<String, Object> existing = new LinkedHashMap<>(Map.of("id", "d1", "org_id", "o1", "status", "draft"));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(existing));

        store.updateByQuery("projects", Map.of("id", "d1"), Map.of("status", "active"), true);

        verify(jdbc, atLeastOnce()).update(contains("INSERT INTO documents"),
                eq("projects"), eq("d1"), eq("o1"), contains("active"));
    }

    @Test
    void nowIso_returnsIsoFormattedString() {
        String now = SqlDocumentStore.nowIso();
        assertNotNull(now);
        assertTrue(now.contains("T"), "Should be ISO format with T separator");
    }

    @Test
    void nowTs_returnsCurrentTimestamp() {
        assertNotNull(SqlDocumentStore.nowTs());
    }
}
