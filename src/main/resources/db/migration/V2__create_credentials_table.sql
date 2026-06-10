CREATE TABLE credentials (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    provider   VARCHAR(20)  NOT NULL,
    identifier VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_credentials_provider_identifier UNIQUE (provider, identifier),
    CONSTRAINT uk_credentials_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_credentials_user_id ON credentials (user_id);