ALTER TABLE users.users
    DROP CONSTRAINT users_document_type_check,
    ADD CONSTRAINT chk_document_type_valid CHECK (document_type in ('CPF', 'CNPJ'));