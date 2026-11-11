CREATE TABLE learning_events (
    student_id BIGINT NOT NULL,
    event_id CHAR(36) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    letter VARCHAR(3) NOT NULL,
    source VARCHAR(16) NOT NULL,
    review BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at_utc DATETIME(3) NOT NULL,
    received_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY(student_id, event_id),
    INDEX ix_learning_day(student_id, occurred_at_utc),
    FOREIGN KEY(student_id) REFERENCES users(id) ON DELETE CASCADE
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

CREATE TABLE learning_progress (
    student_id BIGINT NOT NULL,
    letter VARCHAR(3) NOT NULL,
    read_done BOOLEAN NOT NULL,
    writes INT NOT NULL,
    words INT NOT NULL,
    sentences INT NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY(student_id, letter),
    FOREIGN KEY(student_id) REFERENCES users(id) ON DELETE CASCADE
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

CREATE TABLE parent_link_invites (
    student_id BIGINT PRIMARY KEY,
    code_hash CHAR(64) NOT NULL UNIQUE,
    expires_at_utc DATETIME(3) NOT NULL,
    FOREIGN KEY(student_id) REFERENCES users(id) ON DELETE CASCADE
);
