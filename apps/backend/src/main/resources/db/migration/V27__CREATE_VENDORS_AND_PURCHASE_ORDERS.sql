CREATE TABLE inventory.vendors
(
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    name          VARCHAR(150) NOT NULL,
    contact_email VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE SEQUENCE IF NOT EXISTS inventory.seq_purchase_order_code
    AS BIGINT
    INCREMENT BY 1;

CREATE TABLE inventory.purchase_orders
(
    id               UUID PRIMARY KEY DEFAULT uuidv7(),
    code             VARCHAR(30) NOT NULL UNIQUE,
    vendor_id        UUID        NOT NULL,
    status           VARCHAR(20) NOT NULL,
    vendor_order_ref VARCHAR(100),
    placed_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    expected_at      TIMESTAMPTZ,
    received_at      TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,

    CONSTRAINT fk_purchase_orders_on_vendors
        FOREIGN KEY (vendor_id)
            REFERENCES inventory.vendors (id),

    CONSTRAINT chk_purchase_order_status_in CHECK (
        status IN ('PLACED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')
        )
);

CREATE TABLE inventory.purchase_order_lines
(
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    purchase_order_id UUID           NOT NULL,
    part_id           UUID           NOT NULL,
    quantity_ordered  DECIMAL(12, 3) NOT NULL CHECK (quantity_ordered > 0),
    quantity_received DECIMAL(12, 3) NOT NULL DEFAULT 0 CHECK (quantity_received >= 0),
    unit_cost         DECIMAL(10, 2) CHECK (unit_cost >= 0),

    CONSTRAINT fk_purchase_order_lines_on_purchase_orders
        FOREIGN KEY (purchase_order_id)
            REFERENCES inventory.purchase_orders (id),

    CONSTRAINT fk_purchase_order_lines_on_parts
        FOREIGN KEY (part_id)
            REFERENCES inventory.parts (id),

    -- Mirrors chk_reserved_not_exceeding_requested on part_reservations: a line can never receive
    -- more than it ordered.
    CONSTRAINT chk_received_not_exceeding_ordered CHECK (quantity_received <= quantity_ordered)
);

CREATE INDEX idx_purchase_order_lines_on_purchase_order_id ON inventory.purchase_order_lines (purchase_order_id);
CREATE INDEX idx_purchase_orders_on_vendor_id ON inventory.purchase_orders (vendor_id);
CREATE INDEX idx_purchase_orders_on_status ON inventory.purchase_orders (status);
