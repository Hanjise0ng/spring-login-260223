CREATE TABLE users (
    user_id    BIGINT       NOT NULL AUTO_INCREMENT,
    public_id  VARCHAR(36)  NOT NULL,
    nickname   VARCHAR(50)  NOT NULL,
    tag        VARCHAR(4)   NOT NULL,
    email      VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    deleted_at DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_public_id UNIQUE (public_id),
    CONSTRAINT uk_users_nickname_tag UNIQUE (nickname, tag)
);

CREATE INDEX idx_users_nickname_tag ON users (nickname, tag);

CREATE TABLE credentials (
    credential_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    provider      VARCHAR(20)  NOT NULL,
    identifier    VARCHAR(100) NOT NULL,
    password      VARCHAR(255) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (credential_id),
    CONSTRAINT uk_credentials_provider_identifier UNIQUE (provider, identifier),
    CONSTRAINT uk_credentials_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_credentials_user_id ON credentials (user_id);

CREATE TABLE devices (
    device_id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id            BIGINT      NOT NULL,
    public_id          VARCHAR(36) NOT NULL,
    device_fingerprint VARCHAR(64) NOT NULL,
    device_type        VARCHAR(20) NOT NULL,
    os_name            VARCHAR(50) NOT NULL,
    browser_name       VARCHAR(50) NOT NULL,
    last_login_ip      VARCHAR(45) NOT NULL,
    last_login_at      DATETIME(6) NOT NULL,
    session_id         VARCHAR(36) NULL,
    trusted            BIT(1)      NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (device_id),
    CONSTRAINT uk_devices_public_id UNIQUE (public_id),
    CONSTRAINT uk_devices_user_fingerprint UNIQUE (user_id, device_fingerprint)
);

CREATE INDEX idx_devices_user_last_login ON devices (user_id, last_login_at DESC);
CREATE INDEX idx_devices_session_id ON devices (session_id);

CREATE TABLE login_attempts (
    login_attempt_id   BIGINT      NOT NULL AUTO_INCREMENT,
    user_id            BIGINT      NOT NULL,
    success            BIT(1)      NOT NULL,
    ip                 VARCHAR(45) NOT NULL,
    device_fingerprint VARCHAR(64) NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (login_attempt_id)
);

CREATE INDEX idx_login_attempts_user_created ON login_attempts (user_id, created_at DESC);