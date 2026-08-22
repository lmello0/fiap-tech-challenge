ALTER TABLE work_orders.order_rows
    ADD COLUMN started_at TIMESTAMPTZ,
    ADD COLUMN finished_at TIMESTAMPTZ;

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
