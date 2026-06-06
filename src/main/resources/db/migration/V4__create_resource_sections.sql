CREATE TABLE resource_sections (
    resource_section_id BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    resource_id         BIGINT       NOT NULL,
    title               VARCHAR(255) NOT NULL,
    section_order       INTEGER      NOT NULL,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_resource_sections_resource_id
        FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT chk_resource_sections_section_order
        CHECK (section_order >= 1)
);

CREATE UNIQUE INDEX uq_resource_sections_user_resource_order
    ON resource_sections (user_id, resource_id, section_order);

CREATE INDEX idx_resource_sections_user_resource_deleted
    ON resource_sections (user_id, resource_id, deleted_at);

CREATE INDEX idx_resource_sections_resource_order
    ON resource_sections (resource_id, section_order);
