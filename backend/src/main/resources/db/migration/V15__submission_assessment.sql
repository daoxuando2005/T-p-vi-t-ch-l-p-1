ALTER TABLE submissions
    ADD COLUMN auto_score FLOAT NULL,
    ADD COLUMN grade_source VARCHAR(20) NULL,
    ADD COLUMN assessment_data LONGTEXT NULL;
