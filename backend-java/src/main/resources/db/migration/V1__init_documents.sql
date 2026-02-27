CREATE TABLE IF NOT EXISTS documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_name VARCHAR(128) NOT NULL,
    doc_id VARCHAR(128) NOT NULL,
    org_id VARCHAR(128) NULL,
    json_data JSON NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_collection_doc (collection_name, doc_id),
    INDEX idx_collection_org (collection_name, org_id),
    INDEX idx_collection_docid (collection_name, doc_id)
);
