-- Rebuilds Inventory around a derived stock ledger (see ADR 0018): parts.quantity_on_hand,
-- quantity_reserved, and average_cost are gone. On-hand is SUM(stock_movements.quantity); available
-- is on-hand minus what's HELD in part_reservations; cost is a windowed average over PURCHASE
-- movements. reorder_rules becomes stock_policies, splitting the low-stock threshold from the
-- optional auto-reorder behavior (see ADR 0020). The application is not live yet — no data here is
-- worth migrating (see ADR 0018's context).
DROP SCHEMA inventory CASCADE;

CREATE SCHEMA inventory;

CREATE TABLE inventory.parts
(
    id              UUID PRIMARY KEY     DEFAULT uuidv7(),
    sku             VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     VARCHAR(2000),
    brand           VARCHAR(100),
    unit_of_measure VARCHAR(20)  NOT NULL,
    sale_price      DECIMAL(10, 2) NOT NULL CHECK (sale_price >= 0),
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,

    CONSTRAINT chk_unit_of_measure_in CHECK (
        unit_of_measure IN ('UNIT', 'LITER', 'KILOGRAM', 'METER', 'SET')
        )
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

CREATE TABLE inventory.vendors
(
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    name          VARCHAR(150) NOT NULL,
    contact_email VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE SEQUENCE inventory.seq_purchase_order_code
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

    CONSTRAINT chk_received_not_exceeding_ordered CHECK (quantity_received <= quantity_ordered)
);

CREATE INDEX idx_purchase_order_lines_on_purchase_order_id ON inventory.purchase_order_lines (purchase_order_id);
CREATE INDEX idx_purchase_orders_on_vendor_id ON inventory.purchase_orders (vendor_id);
CREATE INDEX idx_purchase_orders_on_status ON inventory.purchase_orders (status);

-- The only source of truth for stock. On-hand for a part is SUM(quantity) over its rows here; there
-- is no column anywhere that stores it directly. Each movement type carries exactly the source
-- reference that explains it, enforced below.
CREATE TABLE inventory.stock_movements
(
    id                     UUID PRIMARY KEY        DEFAULT uuidv7(),
    part_id                UUID           NOT NULL,
    type                   VARCHAR(20)    NOT NULL,
    quantity               DECIMAL(12, 3) NOT NULL CHECK (quantity <> 0),
    unit_cost              DECIMAL(10, 2) CHECK (unit_cost >= 0),
    reason                 VARCHAR(500),
    work_order_id          UUID,
    purchase_order_line_id UUID,
    occurred_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_stock_movements_on_parts
        FOREIGN KEY (part_id)
            REFERENCES inventory.parts (id),

    CONSTRAINT fk_stock_movements_on_purchase_order_lines
        FOREIGN KEY (purchase_order_line_id)
            REFERENCES inventory.purchase_order_lines (id),

    CONSTRAINT chk_movement_type_in CHECK (
        type IN ('PURCHASE', 'CONSUMPTION', 'ADJUSTMENT')
        ),

    -- Each type's source reference is required, and no other type's reference may be set alongside it.
    CONSTRAINT chk_movement_source_matches_type CHECK (
        (type = 'PURCHASE' AND unit_cost IS NOT NULL AND purchase_order_line_id IS NOT NULL
            AND reason IS NULL AND work_order_id IS NULL)
            OR
        (type = 'CONSUMPTION' AND work_order_id IS NOT NULL
            AND unit_cost IS NULL AND purchase_order_line_id IS NULL AND reason IS NULL)
            OR
        (type = 'ADJUSTMENT' AND reason IS NOT NULL AND length(trim(reason)) > 0
            AND unit_cost IS NULL AND purchase_order_line_id IS NULL AND work_order_id IS NULL)
        )
);

CREATE INDEX idx_stock_movements_on_part_id ON inventory.stock_movements (part_id);
CREATE INDEX idx_stock_movements_on_work_order_id ON inventory.stock_movements (work_order_id);

CREATE TABLE inventory.part_reservations
(
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    part_id            UUID           NOT NULL,
    work_order_id      UUID           NOT NULL,
    quantity_requested DECIMAL(12, 3) NOT NULL CHECK (quantity_requested >= 0),
    quantity_reserved  DECIMAL(12, 3) NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    status             VARCHAR(20)    NOT NULL,
    reserved_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    resolved_at        TIMESTAMPTZ,

    CONSTRAINT fk_part_reservations_on_parts
        FOREIGN KEY (part_id)
            REFERENCES inventory.parts (id),

    CONSTRAINT chk_reservation_status_in CHECK (
        status IN ('HELD', 'CONSUMED', 'RELEASED', 'EXPIRED')
        ),

    -- A reservation can never hold more than it asked for; the gap (requested - reserved) is the
    -- shortfall, derived rather than stored.
    CONSTRAINT chk_reserved_not_exceeding_requested CHECK (quantity_reserved <= quantity_requested)
);

CREATE INDEX idx_part_reservations_on_work_order_id ON inventory.part_reservations (work_order_id);
CREATE INDEX idx_part_reservations_on_status ON inventory.part_reservations (status);
CREATE INDEX idx_part_reservations_on_part_id_status ON inventory.part_reservations (part_id, status);

-- Splits the low-stock threshold (always meaningful) from the auto-reorder order-up-to policy (only
-- meaningful when auto_reorder_enabled). See ADR 0020.
CREATE TABLE inventory.stock_policies
(
    id                   UUID PRIMARY KEY DEFAULT uuidv7(),
    part_id              UUID           NOT NULL UNIQUE,
    min_quantity         DECIMAL(12, 3) NOT NULL CHECK (min_quantity >= 0),
    max_quantity         DECIMAL(12, 3),
    vendor_id            UUID,
    auto_reorder_enabled BOOLEAN        NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ,

    CONSTRAINT fk_stock_policies_on_parts
        FOREIGN KEY (part_id)
            REFERENCES inventory.parts (id),

    CONSTRAINT fk_stock_policies_on_vendors
        FOREIGN KEY (vendor_id)
            REFERENCES inventory.vendors (id),

    -- A policy that could never order a positive quantity (max <= min) would trigger forever without
    -- ever satisfying itself.
    CONSTRAINT chk_stock_policy_max_gt_min CHECK (max_quantity IS NULL OR max_quantity > min_quantity),

    -- max_quantity and vendor_id are only meaningful, and only required, when auto-reorder is on.
    CONSTRAINT chk_auto_reorder_requires_max_and_vendor CHECK (
        NOT auto_reorder_enabled OR (max_quantity IS NOT NULL AND vendor_id IS NOT NULL)
        )
);

CREATE TABLE inventory.service_executions
(
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    repair_service_id UUID        NOT NULL,
    work_order_id     UUID        NOT NULL,
    work_order_row_id UUID        NOT NULL UNIQUE,
    duration_seconds  INTEGER     NOT NULL CHECK (duration_seconds > 0),
    recorded_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_service_executions_on_repair_services
        FOREIGN KEY (repair_service_id)
            REFERENCES inventory.repair_services (id)
);

CREATE INDEX idx_service_executions_on_repair_service_id ON inventory.service_executions (repair_service_id);

-- Derived read model: everything about a part's stock that must never be a stored column. on_hand
-- and reserved are aggregates over the ledger and live reservations; available is their difference;
-- stock_status classifies a part against its stock_policy (or NO_POLICY if it has none); the four
-- cost columns are moving averages over PURCHASE movements in fixed windows (see ADR 0018 and 0020).
CREATE VIEW inventory.vw_part_stock AS
SELECT p.id                                                              AS part_id,
       COALESCE(sm.on_hand, 0)                                           AS on_hand,
       COALESCE(pr.reserved, 0)                                          AS reserved,
       COALESCE(sm.on_hand, 0) - COALESCE(pr.reserved, 0)                AS available,
       CASE
           WHEN COALESCE(sm.on_hand, 0) - COALESCE(pr.reserved, 0) <= 0 THEN 'OUT'
           WHEN sp.id IS NULL THEN 'NO_POLICY'
           WHEN COALESCE(sm.on_hand, 0) - COALESCE(pr.reserved, 0) <= sp.min_quantity THEN 'LOW'
           ELSE 'OK'
           END                                                           AS stock_status,
       c30.avg_cost                                                      AS avg_cost_30d,
       c90.avg_cost                                                      AS avg_cost_90d,
       c365.avg_cost                                                     AS avg_cost_365d,
       call.avg_cost                                                     AS avg_cost_all_time
FROM inventory.parts p
         LEFT JOIN (SELECT part_id, SUM(quantity) AS on_hand
                    FROM inventory.stock_movements
                    GROUP BY part_id) sm ON sm.part_id = p.id
         LEFT JOIN (SELECT part_id, SUM(quantity_reserved) AS reserved
                    FROM inventory.part_reservations
                    WHERE status = 'HELD'
                    GROUP BY part_id) pr ON pr.part_id = p.id
         LEFT JOIN inventory.stock_policies sp ON sp.part_id = p.id
         LEFT JOIN (SELECT part_id, SUM(quantity * unit_cost) / NULLIF(SUM(quantity), 0) AS avg_cost
                    FROM inventory.stock_movements
                    WHERE type = 'PURCHASE'
                      AND occurred_at >= now() - INTERVAL '30 days'
                    GROUP BY part_id) c30 ON c30.part_id = p.id
         LEFT JOIN (SELECT part_id, SUM(quantity * unit_cost) / NULLIF(SUM(quantity), 0) AS avg_cost
                    FROM inventory.stock_movements
                    WHERE type = 'PURCHASE'
                      AND occurred_at >= now() - INTERVAL '90 days'
                    GROUP BY part_id) c90 ON c90.part_id = p.id
         LEFT JOIN (SELECT part_id, SUM(quantity * unit_cost) / NULLIF(SUM(quantity), 0) AS avg_cost
                    FROM inventory.stock_movements
                    WHERE type = 'PURCHASE'
                      AND occurred_at >= now() - INTERVAL '365 days'
                    GROUP BY part_id) c365 ON c365.part_id = p.id
         LEFT JOIN (SELECT part_id, SUM(quantity * unit_cost) / NULLIF(SUM(quantity), 0) AS avg_cost
                    FROM inventory.stock_movements
                    WHERE type = 'PURCHASE'
                    GROUP BY part_id) call ON call.part_id = p.id;
