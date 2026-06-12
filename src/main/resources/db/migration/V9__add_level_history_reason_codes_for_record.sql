ALTER TABLE level_histories
    DROP CONSTRAINT chk_level_histories_reason_code;

ALTER TABLE level_histories
    ADD CONSTRAINT chk_level_histories_reason_code CHECK (reason_code IN (
        'INITIAL_STUDY_COMPLETED',
        'ALL_SECTIONS_STUDIED',
        'IMMEDIATE_REFLECTION_RECORD_SAVED',
        'DELAYED_RECALL_RECORD_SAVED',
        'IMMEDIATE_REFLECTION_RECORDED',
        'DELAYED_RECALL_RECORDED'
    ));
