CREATE TABLE study_memos (
    study_memo_id       BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    resource_id         BIGINT       NOT NULL,
    resource_section_id BIGINT,
    memo_type           VARCHAR(50)  NOT NULL,
    content             VARCHAR(500) NOT NULL,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_study_memos_resource_id
        FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT fk_study_memos_resource_section_id
        FOREIGN KEY (resource_section_id) REFERENCES resource_sections (resource_section_id),
    CONSTRAINT chk_study_memos_content
        CHECK (char_length(content) BETWEEN 1 AND 500),
    CONSTRAINT chk_study_memos_memo_type
        CHECK (memo_type IN (
            'GENERAL',
            'LEARNED',
            'QUESTION',
            'WEAKNESS',
            'TODO',
            'IDEA',
            'SUMMARY'
        ))
);

CREATE INDEX idx_study_memos_user_resource_deleted
    ON study_memos (user_id, resource_id, deleted_at);

CREATE INDEX idx_study_memos_user_section_deleted
    ON study_memos (user_id, resource_section_id, deleted_at);

CREATE INDEX idx_study_memos_user_resource_created
    ON study_memos (user_id, resource_id, created_at);
