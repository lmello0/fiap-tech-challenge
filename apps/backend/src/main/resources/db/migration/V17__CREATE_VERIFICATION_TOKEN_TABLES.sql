CREATE TABLE auth.password_reset_tokens
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_password_reset_tokens_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_pending
    ON auth.password_reset_tokens (user_id) WHERE used_at IS NULL;

CREATE TABLE auth.email_verification_tokens
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_email_verification_tokens_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user_pending
    ON auth.email_verification_tokens (user_id) WHERE used_at IS NULL;

CREATE TABLE auth.email_change_tokens
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id    UUID         NOT NULL,
    new_email  VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_email_change_tokens_on_users
        FOREIGN KEY (user_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_email_change_tokens_user_pending
    ON auth.email_change_tokens (user_id) WHERE used_at IS NULL;
