-- Redesign: WorkOrder no longer holds items of its own (ADR 0008). A Budget is a separate entity,
-- one active per WorkOrder, with its own send/approve/refuse lifecycle (ADR 0009) and its own lines
-- (BudgetLine, replacing order_rows). Pre-production: no data migration needed, clean rebuild.

DROP TABLE IF EXISTS work_orders.order_rows;

ALTER TABLE work_orders.orders
    DROP COLUMN IF EXISTS labor_total,
    DROP COLUMN IF EXISTS parts_total,
    DROP COLUMN IF EXISTS grand_total;

ALTER TABLE work_orders.orders
    ALTER COLUMN assigned_mechanic_id DROP NOT NULL,
    ADD COLUMN budget_id UUID;

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
                   'DELIVERED'
            )
        );

CREATE TABLE work_orders.budgets
(
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    work_order_id UUID                     NOT NULL,
    status        VARCHAR(15)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    sent_at       TIMESTAMP WITH TIME ZONE,
    resolved_at   TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_budgets_on_orders
        FOREIGN KEY (work_order_id)
            REFERENCES work_orders.orders (id),

    CONSTRAINT chk_budget_status_is_valid CHECK (
        status IN ('DRAFT', 'WAITING_SEND', 'SENT', 'APPROVED', 'REFUSED')
        )
);

ALTER TABLE work_orders.orders
    ADD CONSTRAINT fk_orders_on_budgets
        FOREIGN KEY (budget_id)
            REFERENCES work_orders.budgets (id);

CREATE TABLE work_orders.budget_lines
(
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    budget_id   UUID           NOT NULL,
    type        VARCHAR(10)    NOT NULL CHECK (type IN ('SERVICE', 'PART')),
    description VARCHAR(2000)  NOT NULL,
    quantity    DECIMAL(10, 3) NOT NULL CHECK (quantity >= 0),
    unit_price  DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    part_id     UUID,
    service_id  UUID,
    started_at  TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_budget_lines_on_budgets
        FOREIGN KEY (budget_id)
            REFERENCES work_orders.budgets (id),

    CONSTRAINT chk_budget_line_data CHECK (
        (type = 'PART' AND part_id IS NOT NULL AND service_id IS NULL)
            OR
        (type = 'SERVICE' AND service_id IS NOT NULL AND part_id IS NULL)
        )
);
