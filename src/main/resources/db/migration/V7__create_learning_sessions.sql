CREATE TABLE learning_sessions (
    learning_session_id BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    resource_id         BIGINT       NOT NULL,
    session_type        VARCHAR(50)  NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    current_step        INTEGER      NOT NULL,
    total_steps         INTEGER      NOT NULL,
    started_at          TIMESTAMPTZ  NOT NULL,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_learning_sessions_resource_id
        FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT chk_learning_sessions_session_type
        CHECK (session_type IN (
            'IMMEDIATE_REFLECTION',
            'DELAYED_RECALL'
        )),
    CONSTRAINT chk_learning_sessions_status
        CHECK (status IN (
            'IN_PROGRESS',
            'COMPLETED',
            'RECORD_SAVED',
            'DISCARDED'
        )),
    CONSTRAINT chk_learning_sessions_current_step
        CHECK (current_step >= 1),
    CONSTRAINT chk_learning_sessions_total_steps
        CHECK (total_steps >= 1),
    CONSTRAINT chk_learning_sessions_step_range
        CHECK (current_step <= total_steps)
);

CREATE INDEX idx_learning_sessions_user_resource_session_status
    ON learning_sessions (user_id, resource_id, session_type, status);

CREATE INDEX idx_learning_sessions_resource_id
    ON learning_sessions (resource_id);

CREATE UNIQUE INDEX uq_learning_sessions_active
    ON learning_sessions (user_id, resource_id, session_type)
    WHERE status IN ('IN_PROGRESS', 'COMPLETED');
