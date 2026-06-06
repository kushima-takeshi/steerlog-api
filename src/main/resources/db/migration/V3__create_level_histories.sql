CREATE TABLE level_histories (
    level_history_id BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    resource_id      BIGINT       NOT NULL,
    level            INTEGER      NOT NULL,
    source_type      VARCHAR(50)  NOT NULL,
    source_id        BIGINT,
    reason_code      VARCHAR(100) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_level_histories_resource_id
        FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT chk_level_histories_level
        CHECK (level BETWEEN 1 AND 5),
    CONSTRAINT chk_level_histories_source_type CHECK (source_type IN (
        'INITIAL_STUDY_COMPLETION',
        'SECTION_STUDY_STATUS',
        'LEARNING_SESSION_RECORD'
    )),
    CONSTRAINT chk_level_histories_reason_code CHECK (reason_code IN (
        'INITIAL_STUDY_COMPLETED',
        'ALL_SECTIONS_STUDIED',
        'IMMEDIATE_REFLECTION_RECORD_SAVED',
        'DELAYED_RECALL_RECORD_SAVED'
    ))
);

CREATE UNIQUE INDEX uq_level_histories_user_resource_level
    ON level_histories (user_id, resource_id, level);

CREATE INDEX idx_level_histories_user_resource
    ON level_histories (user_id, resource_id);

CREATE INDEX idx_level_histories_source
    ON level_histories (source_type, source_id);
