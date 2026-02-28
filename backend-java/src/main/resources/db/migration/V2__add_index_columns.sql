ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS status_col   VARCHAR(64)  GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(json_data, '$.status')))   VIRTUAL,
    ADD COLUMN IF NOT EXISTS created_at_col VARCHAR(32) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(json_data, '$.created_at'))) VIRTUAL;

CREATE INDEX IF NOT EXISTS idx_collection_org_status
    ON documents (collection_name, org_id, status_col);

CREATE INDEX IF NOT EXISTS idx_collection_org_created
    ON documents (collection_name, org_id, created_at_col);
