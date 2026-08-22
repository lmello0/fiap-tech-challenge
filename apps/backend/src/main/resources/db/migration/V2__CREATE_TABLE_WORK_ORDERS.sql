CREATE SCHEMA work_orders;

CREATE TABLE work_orders.orders
(
    id                   UUID PRIMARY KEY DEFAULT uuidv7(),
    order_number         VARCHAR(30)              NOT NULL UNIQUE,
    status               VARCHAR(25)              NOT NULL,
    customer_id          UUID                     NOT NULL,
    vehicle_id           UUID                     NOT NULL,
    assigned_mechanic_id UUID                     NOT NULL,
    customer_complaint   VARCHAR(2000),
    diagnosis            VARCHAR(2000),
    refusal_reason       VARCHAR(2000),
    labor_total          DECIMAL(10, 2) CHECK (labor_total >= 0),
    parts_total          DECIMAL(10, 2) CHECK (parts_total >= 0),
    discount             DECIMAL(10, 2) CHECK (discount >= 0),
    tax_total            DECIMAL(10, 2) CHECK (tax_total >= 0),
    grand_total          DECIMAL(10, 2) CHECK (grand_total >= 0),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE,
    approved_at          TIMESTAMP WITH TIME ZONE,
    refused_at           TIMESTAMP WITH TIME ZONE,
    finished_at          TIMESTAMP WITH TIME ZONE,
    delivered_at         TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_status_is_valid CHECK (
        status IN (
                   'RECEIVED',
                   'WAITING_DIAGNOSTICS',
                   'IN_DIAGNOSTICS',
                   'WAITING_APPROVAL',
                   'APPROVED',
                   'REFUSED',
                   'IN_PROGRESS',
                   'FINISHED',
                   'WAITING_PICKUP',
                   'DELIVERED'
            )
        )
);

CREATE TABLE work_orders.order_rows
(
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    work_order_id UUID           NOT NULL,
    type          VARCHAR(10)    NOT NULL CHECK (type in ('SERVICE', 'PART')),
    description   VARCHAR(2000),
    quantity      DECIMAL(10, 3) CHECK (quantity >= 0),
    unit_price    DECIMAL(10, 2) CHECK (unit_price >= 0),
    line_total    DECIMAL(10, 2) NOT NULL CHECK (line_total >= 0),
    part_id       UUID,

    CONSTRAINT fk_order_rows_on_orders
        FOREIGN KEY (work_order_id)
            REFERENCES work_orders.orders (id),

    CONSTRAINT chk_order_row_data check (
        (type = 'PART'
            AND part_id IS NOT NULL
            AND quantity IS NOT NULL
            AND unit_price IS NOT NULL)
            OR
        (type = 'SERVICE'
            AND part_id IS NULL
            AND quantity IS NULL
            AND unit_price IS NULL)
        ),

    CONSTRAINT chk_line_total_is_derived check (
        type = 'SERVICE' OR line_total = quantity * unit_price
        )
);
