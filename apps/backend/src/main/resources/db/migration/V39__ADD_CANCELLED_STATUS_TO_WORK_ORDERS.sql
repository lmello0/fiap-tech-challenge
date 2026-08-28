ALTER TABLE work_orders.orders
    ADD COLUMN cancel_reason VARCHAR(2000),
    ADD COLUMN cancelled_at  TIMESTAMP WITH TIME ZONE;

ALTER TABLE work_orders.orders
    DROP CONSTRAINT IF EXISTS chk_status_is_valid;

ALTER TABLE work_orders.orders
    ADD CONSTRAINT chk_status_is_valid CHECK (
        status IN (
                   'RECEIVED',
                   'WAITING_DIAGNOSTICS',
                   'IN_DIAGNOSTICS',
                   'BUDGET_IN_DRAFT',
                   'WAITING_APPROVAL',
                   'APPROVED',
                   'REFUSED',
                   'IN_PROGRESS',
                   'FINISHED',
                   'WAITING_PICKUP',
                   'DELIVERED',
                   'CANCELLED'
            )
        );
