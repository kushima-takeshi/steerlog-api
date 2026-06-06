CREATE TABLE section_study_statuses (
    section_study_status_id BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT      NOT NULL,
    resource_id             BIGINT      NOT NULL,
    resource_section_id     BIGINT      NOT NULL,
    studied_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_section_study_statuses_resource_id
        FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT fk_section_study_statuses_resource_section_id
        FOREIGN KEY (resource_section_id) REFERENCES resource_sections (resource_section_id)
);

CREATE UNIQUE INDEX uq_section_study_statuses_user_section
    ON section_study_statuses (user_id, resource_section_id);

CREATE INDEX idx_section_study_statuses_user_resource
    ON section_study_statuses (user_id, resource_id);

CREATE INDEX idx_section_study_statuses_user_section
    ON section_study_statuses (user_id, resource_section_id);
