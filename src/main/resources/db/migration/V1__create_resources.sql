CREATE TABLE resources (
    resource_id   BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    author        VARCHAR(255),
    source_url    TEXT,
    description   TEXT,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_resources_resource_type CHECK (resource_type IN (
        'BOOK',
        'ARTICLE',
        'VIDEO',
        'COURSE',
        'PROBLEM',
        'IMPLEMENTATION',
        'DOCUMENTATION',
        'OTHER'
    ))
);

CREATE INDEX idx_resources_user_id
    ON resources (user_id);

CREATE INDEX idx_resources_user_deleted
    ON resources (user_id, deleted_at);
