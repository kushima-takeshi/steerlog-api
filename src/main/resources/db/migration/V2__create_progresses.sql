CREATE TABLE progresses (
    progress_id        BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    resource_id        BIGINT       NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    current_level      INTEGER      NOT NULL,
    current_section_id BIGINT,
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    archived_at        TIMESTAMPTZ,
    archive_reason     VARCHAR(500),
    initial_studied_at TIMESTAMPTZ,
    last_studied_at    TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_progresses_resource_id
        FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT chk_progresses_current_level
        CHECK (current_level BETWEEN 0 AND 5),
    CONSTRAINT chk_progresses_status CHECK (status IN (
        'NOT_STARTED',
        'IN_PROGRESS',
        'PAUSED',
        'ARCHIVED',
        'COMPLETED'
    ))
);

CREATE UNIQUE INDEX uq_progresses_user_resource
    ON progresses (user_id, resource_id);
