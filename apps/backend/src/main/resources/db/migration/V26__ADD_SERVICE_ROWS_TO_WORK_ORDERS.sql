-- Prior state: order_rows_type_check allows ('LABOR', 'PART') as of V8, but chk_order_row_data (from
-- V2, never updated) only ever recognized ('PART', 'SERVICE'). A row of type LABOR satisfies neither
-- branch of chk_order_row_data, so it has been impossible to insert since V2 — the SERVICE row type
-- this migration introduces is what the app has needed all along.

ALTER TABLE work_orders.order_rows
    ADD COLUMN service_id UUID;

ALTER TABLE work_orders.order_rows
    DROP CONSTRAINT order_rows_type_check,
    ADD CONSTRAINT order_rows_type_check CHECK (type IN ('SERVICE', 'PART'));

ALTER TABLE work_orders.order_rows
    DROP CONSTRAINT chk_order_row_data,
    ADD CONSTRAINT chk_order_row_data CHECK (
        (type = 'PART'
            AND part_id IS NOT NULL
            AND service_id IS NULL
            AND quantity IS NOT NULL
            AND unit_price IS NOT NULL)
            OR
        (type = 'SERVICE'
            AND service_id IS NOT NULL
            AND part_id IS NULL
            AND quantity IS NOT NULL
            AND unit_price IS NOT NULL)
        );
