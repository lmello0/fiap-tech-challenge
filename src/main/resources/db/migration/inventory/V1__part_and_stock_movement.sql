CREATE TABLE part (
    id               UUID                     PRIMARY KEY,
    sku              VARCHAR(40)              NOT NULL,
    name             VARCHAR(120)             NOT NULL,
    description      VARCHAR(500),
    unit_price       NUMERIC(12, 2)           NOT NULL CHECK (unit_price >= 0),
    minimum_quantity INTEGER                  NOT NULL CHECK (minimum_quantity >= 0),
    on_hand          INTEGER                  NOT NULL CHECK (on_hand >= 0),
    active           BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uk_part_sku UNIQUE (sku)
);

CREATE TABLE stock_movement (
    id            UUID                     PRIMARY KEY,
    part_id       UUID                     NOT NULL REFERENCES part (id),
    type          VARCHAR(20)              NOT NULL,
    quantity      INTEGER                  NOT NULL CHECK (quantity <> 0),
    balance_after INTEGER                  NOT NULL CHECK (balance_after >= 0),
    reason        VARCHAR(255),
    performed_by  VARCHAR(100),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_stock_movement_part ON stock_movement (part_id, created_at DESC);
