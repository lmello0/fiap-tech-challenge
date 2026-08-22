CREATE SCHEMA inventory;

CREATE TABLE inventory.parts
(
    id                UUID PRIMARY KEY        DEFAULT uuidv7(),
    sku               VARCHAR(50)    NOT NULL UNIQUE,
    name              VARCHAR(150)   NOT NULL,
    description       VARCHAR(2000),
    brand             VARCHAR(100),
    unit_of_measure   VARCHAR(20)    NOT NULL,
    sale_price        DECIMAL(10, 2) NOT NULL CHECK (sale_price >= 0),
    average_cost      DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (average_cost >= 0),
    quantity_on_hand  DECIMAL(12, 3) NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved DECIMAL(12, 3) NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    active            BOOLEAN        NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ,

    CONSTRAINT chk_unit_of_measure_in CHECK (
        unit_of_measure IN ('UNIT', 'LITER', 'KILOGRAM', 'METER', 'SET')
        ),

    -- Reserved stock is always a claim on stock that exists; a reservation is never created for
    -- more than what is on hand at the moment it's placed (see reservation design in inventory).
    CONSTRAINT chk_reserved_not_exceeding_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE TABLE inventory.repair_services
(
    id                UUID PRIMARY KEY        DEFAULT uuidv7(),
    code              VARCHAR(50)    NOT NULL UNIQUE,
    name              VARCHAR(150)   NOT NULL,
    description       VARCHAR(2000),
    price             DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    estimated_seconds INTEGER        NOT NULL CHECK (estimated_seconds > 0),
    average_seconds   INTEGER CHECK (average_seconds > 0),
    execution_count   INTEGER        NOT NULL DEFAULT 0 CHECK (execution_count >= 0),
    active            BOOLEAN        NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ
);

CREATE TABLE inventory.stock_movements
(
    id           UUID PRIMARY KEY        DEFAULT uuidv7(),
    part_id      UUID           NOT NULL,
    type         VARCHAR(20)    NOT NULL,
    quantity     DECIMAL(12, 3) NOT NULL CHECK (quantity <> 0),
    unit_cost    DECIMAL(10, 2) CHECK (unit_cost >= 0),
    reference_id UUID,
    reason       VARCHAR(500),
    occurred_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_stock_movements_on_parts
        FOREIGN KEY (part_id)
            REFERENCES inventory.parts (id),

    CONSTRAINT chk_movement_type_in CHECK (
        type IN ('PURCHASE', 'CONSUMPTION', 'ADJUSTMENT')
        ),

    -- A manual adjustment must always be explained; receipts and consumption are self-explanatory
    -- from their reference_id and need no free-text reason.
    CONSTRAINT chk_adjustment_has_reason CHECK (
        type <> 'ADJUSTMENT' OR (reason IS NOT NULL AND length(trim(reason)) > 0)
        )
);

CREATE INDEX idx_stock_movements_on_part_id ON inventory.stock_movements (part_id);
