package com.constructiq.backendjava.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SqlDocumentStore {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public SqlDocumentStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void upsert(String collection, Map<String, Object> doc) {
        String docId = String.valueOf(doc.get("id"));
        String orgId = doc.get("org_id") == null ? null : String.valueOf(doc.get("org_id"));
        String json = toJson(doc);

        jdbc.update("""
                INSERT INTO documents(collection_name, doc_id, org_id, json_data)
                VALUES (?, ?, ?, CAST(? AS JSON))
                ON DUPLICATE KEY UPDATE org_id = VALUES(org_id), json_data = VALUES(json_data), updated_at = CURRENT_TIMESTAMP
                """, collection, docId, orgId, json);
    }

    public Optional<Map<String, Object>> findOne(String collection, String docId) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT json_data FROM documents WHERE collection_name=? AND doc_id=? LIMIT 1",
                (rs, rowNum) -> toMap(rs.getString("json_data")),
                collection, docId
        );
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> findOne(String collection, String docId, String orgId) {
        Optional<Map<String, Object>> doc = findOne(collection, docId);
        return doc.filter(d -> Objects.equals(value(d, "org_id"), orgId));
    }

    public long deleteOne(String collection, String docId, String orgId) {
        return jdbc.update("DELETE FROM documents WHERE collection_name=? AND doc_id=? AND (org_id <=> ?)", collection, docId, orgId);
    }

    public List<Map<String, Object>> findAll(String collection) {
        return jdbc.query(
                "SELECT json_data FROM documents WHERE collection_name=?",
                (rs, rowNum) -> toMap(rs.getString("json_data")),
                collection
        );
    }

    public long deleteByQuery(String collection, Map<String, Object> query, boolean single) {
        List<Map<String, Object>> matched = filter(collection, query);
        long deleted = 0;
        for (Map<String, Object> doc : matched) {
            Object idObj = doc.get("id");
            if (idObj == null) {
                continue;
            }
            deleted += jdbc.update("DELETE FROM documents WHERE collection_name=? AND doc_id=?", collection, String.valueOf(idObj));
            if (single && deleted > 0) {
                break;
            }
        }
        return deleted;
    }

    public long updateByQuery(String collection, Map<String, Object> query, Map<String, Object> updates, boolean single) {
        List<Map<String, Object>> matched = filter(collection, query);
        long updated = 0;
        for (Map<String, Object> doc : matched) {
            Object idObj = doc.get("id");
            if (idObj == null) {
                continue;
            }
            doc.putAll(updates);
            upsert(collection, doc);
            updated++;
            if (single) {
                break;
            }
        }
        return updated;
    }

    public long count(String collection, Map<String, Object> query) {
        return filter(collection, query).size();
    }

    public List<Map<String, Object>> find(String collection, Map<String, Object> query, String sortField, boolean desc, int skip, int limit) {
        List<Map<String, Object>> docs = filter(collection, query);
        if (sortField != null && !sortField.isBlank()) {
            Comparator<Map<String, Object>> comparator = (left, right) -> {
                Comparable<Object> a = comparableValue(left.get(sortField));
                Comparable<Object> b = comparableValue(right.get(sortField));
                return a.compareTo(b);
            };
            if (desc) {
                comparator = comparator.reversed();
            }
            docs.sort(comparator);
        }

        int from = Math.max(0, skip);
        if (from >= docs.size()) {
            return new ArrayList<>();
        }

        int to = limit <= 0 ? docs.size() : Math.min(docs.size(), from + limit);
        return new ArrayList<>(docs.subList(from, to));
    }

    public List<String> distinct(String collection, String field, Map<String, Object> query) {
        return filter(collection, query).stream()
                .map(d -> d.get(field))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    public void updateFields(String collection, String docId, Map<String, Object> updates) {
        Optional<Map<String, Object>> existingOpt = findOne(collection, docId);
        if (existingOpt.isEmpty()) {
            return;
        }
        Map<String, Object> existing = existingOpt.get();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            existing.put(entry.getKey(), entry.getValue());
        }
        upsert(collection, existing);
    }

    private List<Map<String, Object>> filter(String collection, Map<String, Object> query) {
        List<Map<String, Object>> rows = findAll(collection);
        return rows.stream().filter(doc -> matches(doc, query)).collect(Collectors.toList());
    }

    private boolean matches(Map<String, Object> doc, Map<String, Object> query) {
        for (Map.Entry<String, Object> e : query.entrySet()) {
            String field = e.getKey();
            Object expected = e.getValue();
            Object actual = doc.get(field);

            if (expected instanceof Map<?, ?> opMap) {
                if (opMap.containsKey("$regex")) {
                    String pattern = String.valueOf(opMap.get("$regex")).toLowerCase(Locale.ROOT);
                    String actualStr = actual == null ? "" : String.valueOf(actual).toLowerCase(Locale.ROOT);
                    if (!actualStr.contains(pattern)) {
                        return false;
                    }
                    continue;
                }
                if (opMap.containsKey("$in")) {
                    Object inObj = opMap.get("$in");
                    if (inObj instanceof Collection<?> inValues) {
                        boolean found = inValues.stream().map(String::valueOf).anyMatch(v -> Objects.equals(v, String.valueOf(actual)));
                        if (!found) {
                            return false;
                        }
                        continue;
                    }
                }
                if (opMap.containsKey("$gte")) {
                    String min = String.valueOf(opMap.get("$gte"));
                    String curr = actual == null ? "" : String.valueOf(actual);
                    if (curr.compareTo(min) < 0) {
                        return false;
                    }
                    continue;
                }
                return false;
            }

            if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
                return false;
            }
        }
        return true;
    }

    private String value(Map<String, Object> doc, String key) {
        Object v = doc.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private Map<String, Object> toMap(String json) {
        try {
            return mapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Comparable<Object> comparableValue(Object value) {
        if (value == null) {
            return (Comparable<Object>) (Object) "";
        }
        if (value instanceof Comparable<?> c) {
            return (Comparable<Object>) c;
        }
        return (Comparable<Object>) (Object) String.valueOf(value);
    }

    public static String nowIso() {
        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    public static Timestamp nowTs() {
        return Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
    }
}
