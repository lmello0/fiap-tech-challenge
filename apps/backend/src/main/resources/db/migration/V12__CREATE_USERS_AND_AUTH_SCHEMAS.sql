CREATE SCHEMA users;
CREATE SCHEMA auth;

CREATE TABLE users.users
(
    id            UUID PRIMARY KEY      DEFAULT uuidv7(),
    first_name    VARCHAR(30)  NOT NULL,
    last_name     VARCHAR(30),
    email         VARCHAR(255) NOT NULL,
    document_type VARCHAR(20)  NOT NULL CHECK (document_type IN ('CPF', 'CNPJ', 'PASSPORT', 'RG')),
    document_code VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_document UNIQUE (document_type, document_code),

    CONSTRAINT chk_document_code_is_correct_length CHECK (
        (document_type = 'CNPJ' AND length(document_code) = 14) OR
        (document_type = 'CPF' AND length(document_code) = 11) OR
        (document_type = 'PASSPORT' AND length(document_code) = 8) OR
        (document_type = 'RG' AND length(document_code) = 9)
        )
);

CREATE TABLE users.phone_numbers
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID        NOT NULL,
    type       VARCHAR(12) NOT NULL CHECK (type IN ('MOBILE', 'COMMERCIAL', 'HOME', 'OTHER')),
    phone      VARCHAR(30) NOT NULL,
    is_primary BOOLEAN     NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_phone_numbers_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_phone_user UNIQUE (user_id, phone)
);

CREATE UNIQUE INDEX uk_phone_primary_per_user
    ON users.phone_numbers (user_id) WHERE is_primary;

CREATE TABLE users.customers
(
    user_id    UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_customers_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE
);

CREATE TABLE users.workers
(
    user_id          UUID PRIMARY KEY,
    registration     VARCHAR(50) NOT NULL UNIQUE,
    role             VARCHAR(50) NOT NULL CHECK (role IN ('MECHANIC', 'ATTENDANT', 'MANAGER')),
    hire_date        DATE        NOT NULL,
    termination_date DATE,
    is_active        BOOLEAN     NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ,

    CONSTRAINT fk_workers_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_termination_date_is_greater_than_hire_date CHECK (
        termination_date IS NULL OR termination_date >= hire_date
        )
);

CREATE TABLE auth.user_auths
(
    user_id       UUID        NOT NULL,
    provider      VARCHAR(20) NOT NULL CHECK (provider IN ('LOCAL')),
    provider_id   VARCHAR(255),
    password_hash VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ,

    CONSTRAINT pk_user_auths PRIMARY KEY (user_id, provider),

    CONSTRAINT fk_user_auths_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_local_provider_has_password CHECK (
        provider <> 'LOCAL' OR password_hash IS NOT NULL
        )
);

CREATE TABLE auth.refresh_tokens
(
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id     UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    issued_at   TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID,

    CONSTRAINT fk_refresh_tokens_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_active
    ON auth.refresh_tokens (user_id) WHERE revoked_at IS NULL;

CREATE INDEX idx_refresh_tokens_expires_at
    ON auth.refresh_tokens (expires_at);
