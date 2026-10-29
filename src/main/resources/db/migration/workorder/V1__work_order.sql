CREATE SEQUENCE work_order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE work_order (
    id                UUID                     PRIMARY KEY,
    number            VARCHAR(20)              NOT NULL,
    status            VARCHAR(20)              NOT NULL,
    customer_id       UUID                     NOT NULL,
    customer_name     VARCHAR(150)             NOT NULL,
    customer_document VARCHAR(14)              NOT NULL,
    customer_email    VARCHAR(254)             NOT NULL,
    vehicle_id        UUID                     NOT NULL,
    vehicle_plate     VARCHAR(7)               NOT NULL,
    vehicle_brand     VARCHAR(60)              NOT NULL,
    vehicle_model     VARCHAR(60)              NOT NULL,
    vehicle_year      INTEGER                  NOT NULL,
    complaint         VARCHAR(1000),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uk_work_order_number UNIQUE (number)
);

CREATE UNIQUE INDEX uk_work_order_active_vehicle ON work_order (vehicle_id) WHERE status NOT IN ('DELIVERED', 'CANCELLED');
CREATE INDEX idx_work_order_customer ON work_order (customer_id);
CREATE INDEX idx_work_order_status ON work_order (status);

CREATE TABLE work_order_item (
    id            UUID           PRIMARY KEY,
    work_order_id UUID           NOT NULL REFERENCES work_order (id),
    position      INTEGER        NOT NULL,
    type          VARCHAR(10)    NOT NULL,
    reference_id  UUID           NOT NULL,
    name          VARCHAR(120)   NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    quantity      INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_work_order_item_work_order ON work_order_item (work_order_id);
CREATE INDEX idx_work_order_item_reference ON work_order_item (type, reference_id);

CREATE TABLE budget (
    id                 UUID                     PRIMARY KEY,
    work_order_id      UUID                     NOT NULL REFERENCES work_order (id),
    sequence           INTEGER                  NOT NULL,
    kind               VARCHAR(10)              NOT NULL,
    status             VARCHAR(10)              NOT NULL,
    previous_budget_id UUID REFERENCES budget (id),
    total              NUMERIC(12, 2)           NOT NULL CHECK (total >= 0),
    issued_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at         TIMESTAMP WITH TIME ZONE,
    decided_by         VARCHAR(100),
    decided_role       VARCHAR(20),
    decision_channel   VARCHAR(10),
    decision_reason    VARCHAR(500),
    CONSTRAINT uk_budget_sequence UNIQUE (work_order_id, sequence)
);

CREATE TABLE budget_line (
    id           UUID           PRIMARY KEY,
    budget_id    UUID           NOT NULL REFERENCES budget (id),
    position     INTEGER        NOT NULL,
    type         VARCHAR(10)    NOT NULL,
    reference_id UUID           NOT NULL,
    name         VARCHAR(120)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    quantity     INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_budget_line_budget ON budget_line (budget_id);

CREATE TABLE work_order_status_history (
    id            UUID                     PRIMARY KEY,
    work_order_id UUID                     NOT NULL REFERENCES work_order (id),
    from_status   VARCHAR(20),
    to_status     VARCHAR(20)              NOT NULL,
    occurred_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_subject VARCHAR(100)             NOT NULL,
    actor_role    VARCHAR(20)              NOT NULL,
    note          VARCHAR(500)
);

CREATE INDEX idx_status_history_work_order ON work_order_status_history (work_order_id, occurred_at);
