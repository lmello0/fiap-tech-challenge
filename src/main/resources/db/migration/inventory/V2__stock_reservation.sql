ALTER TABLE part ADD COLUMN reserved INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0);
ALTER TABLE part ADD CONSTRAINT ck_part_reserved_within_stock CHECK (reserved <= on_hand);

CREATE TABLE stock_reservation (
    id                 UUID                     PRIMARY KEY,
    work_order_id      UUID                     NOT NULL,
    work_order_number  VARCHAR(20)              NOT NULL,
    budget_id          UUID                     NOT NULL,
    status             VARCHAR(10)              NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uk_stock_reservation_budget UNIQUE (budget_id)
);

CREATE INDEX idx_stock_reservation_work_order ON stock_reservation (work_order_id);
CREATE INDEX idx_stock_reservation_status ON stock_reservation (status, created_at);

CREATE TABLE stock_reservation_line (
    id             UUID    PRIMARY KEY,
    reservation_id UUID    NOT NULL REFERENCES stock_reservation (id),
    part_id        UUID    NOT NULL REFERENCES part (id),
    quantity       INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_stock_reservation_line_reservation ON stock_reservation_line (reservation_id);
CREATE INDEX idx_stock_reservation_line_part ON stock_reservation_line (part_id);
