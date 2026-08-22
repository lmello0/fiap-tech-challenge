ALTER TABLE users.customers
    ADD COLUMN active         BOOLEAN     NOT NULL DEFAULT true,
    ADD COLUMN deactivated_at TIMESTAMPTZ;

ALTER TABLE users.users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE users.users
    ALTER COLUMN email_verified SET DEFAULT false;
