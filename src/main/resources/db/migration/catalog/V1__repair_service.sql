CREATE TABLE repair_service (
    id                UUID                     PRIMARY KEY,
    name              VARCHAR(120)             NOT NULL,
    description       VARCHAR(500),
    price             NUMERIC(12, 2)           NOT NULL CHECK (price >= 0),
    estimated_minutes INTEGER                  NOT NULL CHECK (estimated_minutes > 0),
    active            BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_repair_service_name ON repair_service (lower(name));
