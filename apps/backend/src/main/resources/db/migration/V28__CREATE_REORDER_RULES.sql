CREATE TABLE inventory.reorder_rules
(
    id           UUID PRIMARY KEY DEFAULT uuidv7(),
    part_id      UUID           NOT NULL UNIQUE,
    min_quantity DECIMAL(12, 3) NOT NULL CHECK (min_quantity >= 0),
    max_quantity DECIMAL(12, 3) NOT NULL,
    vendor_id    UUID           NOT NULL,
    enabled      BOOLEAN        NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ,

    CONSTRAINT fk_reorder_rules_on_parts
        FOREIGN KEY (part_id)
            REFERENCES inventory.parts (id),

    CONSTRAINT fk_reorder_rules_on_vendors
        FOREIGN KEY (vendor_id)
            REFERENCES inventory.vendors (id),

    -- A rule that could never order a positive quantity (max <= min) would trigger forever without
    -- ever satisfying itself.
    CONSTRAINT chk_reorder_rule_max_gt_min CHECK (max_quantity > min_quantity)
);
