CREATE TABLE login_attempts (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    user_id            BIGINT      NOT NULL,
    success            BIT(1)      NOT NULL,
    ip                 VARCHAR(45) NOT NULL,
    device_fingerprint VARCHAR(64) NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_login_attempts_user_created ON login_attempts (user_id, created_at DESC);