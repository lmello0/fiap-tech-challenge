CREATE TABLE inventory.part_reservations
(
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    part_id            UUID           NOT NULL,
    work_order_id      UUID           NOT NULL,
    quantity_requested DECIMAL(12, 3) NOT NULL CHECK (quantity_requested > 0),
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
