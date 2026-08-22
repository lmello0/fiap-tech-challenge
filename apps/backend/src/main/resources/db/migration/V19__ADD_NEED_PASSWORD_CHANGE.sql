ALTER TABLE auth.user_auths
    ADD COLUMN need_password_change BOOLEAN NOT NULL DEFAULT FALSE;
