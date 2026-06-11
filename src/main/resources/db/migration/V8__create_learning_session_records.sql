CREATE TABLE learning_session_records (
    learning_session_record_id BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT       NOT NULL,
    resource_id              BIGINT       NOT NULL,
    learning_session_id      BIGINT       NOT NULL,
    session_type             VARCHAR(50)  NOT NULL,
    summary                  TEXT         NOT NULL,
    concept_tags             TEXT,
    weak_point_summary       TEXT,
    next_action              TEXT,
    ai_assessment            VARCHAR(50)  NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_learning_session_records_resource_id
        FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT fk_learning_session_records_learning_session_id
        FOREIGN KEY (learning_session_id) REFERENCES learning_sessions (learning_session_id),
    CONSTRAINT chk_learning_session_records_session_type
        CHECK (session_type IN (
            'IMMEDIATE_REFLECTION',
            'DELAYED_RECALL'
        )),
    CONSTRAINT chk_learning_session_records_ai_assessment
        CHECK (ai_assessment IN (
            'PASSED',
            'NEEDS_REVIEW',
            'OFF_TOPIC'
        )),
    CONSTRAINT uq_learning_session_records_learning_session_id
        UNIQUE (learning_session_id)
);

CREATE INDEX idx_learning_session_records_user_resource
    ON learning_session_records (user_id, resource_id);

CREATE INDEX idx_learning_session_records_resource_id
    ON learning_session_records (resource_id);
