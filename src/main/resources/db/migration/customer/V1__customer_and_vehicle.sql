CREATE TABLE customer (
    id         UUID                     PRIMARY KEY,
    name       VARCHAR(150)             NOT NULL,
    document   VARCHAR(14)              NOT NULL,
    email      VARCHAR(254)             NOT NULL,
    phone      VARCHAR(20),
    active     BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_document UNIQUE (document),
    CONSTRAINT uk_customer_email UNIQUE (email)
);

CREATE TABLE vehicle (
    id          UUID                     PRIMARY KEY,
    customer_id UUID                     NOT NULL REFERENCES customer (id),
    plate       VARCHAR(7)               NOT NULL,
    brand       VARCHAR(60)              NOT NULL,
    model       VARCHAR(60)              NOT NULL,
    model_year  INTEGER                  NOT NULL,
    active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uk_vehicle_plate UNIQUE (plate)
);

CREATE INDEX idx_vehicle_customer ON vehicle (customer_id);
