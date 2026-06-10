CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    public_id     VARCHAR(36)  NOT NULL,
    login_id      VARCHAR(30)  NOT NULL,
    password      VARCHAR(255) NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    tag           VARCHAR(4)   NOT NULL,
    email         VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    auth_provider VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_public_id UNIQUE (public_id),
    CONSTRAINT uk_users_login_id UNIQUE (login_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname_tag UNIQUE (nickname, tag)
);

CREATE INDEX idx_users_login_id ON users (login_id);
CREATE INDEX idx_users_nickname_tag ON users (nickname, tag);

CREATE TABLE social_accounts (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    provider       VARCHAR(20)  NOT NULL,
    provider_id    VARCHAR(100) NOT NULL,
    provider_email VARCHAR(100) NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_provider_provider_id UNIQUE (provider, provider_id),
    CONSTRAINT uk_social_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_social_provider_provider_id ON social_accounts (provider, provider_id);

CREATE TABLE devices (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    user_id            BIGINT      NOT NULL,
    public_id          VARCHAR(36) NOT NULL,
    device_fingerprint VARCHAR(64) NOT NULL,
    device_type        VARCHAR(20) NOT NULL,
    os_name            VARCHAR(50) NOT NULL,
    browser_name       VARCHAR(50) NOT NULL,
    last_login_ip      VARCHAR(45) NOT NULL,
    last_login_at      DATETIME(6)  NOT NULL,
    session_id         VARCHAR(36) NULL,
    trusted            BIT(1)      NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_devices_public_id UNIQUE (public_id),
    CONSTRAINT uk_devices_user_fingerprint UNIQUE (user_id, device_fingerprint)
);

CREATE INDEX idx_devices_user_last_login ON devices (user_id, last_login_at DESC);
CREATE INDEX idx_devices_session_id ON devices (session_id);