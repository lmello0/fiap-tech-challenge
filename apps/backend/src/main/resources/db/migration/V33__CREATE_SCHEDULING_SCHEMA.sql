CREATE SCHEMA scheduling;

CREATE TABLE scheduling.appointments
(
    id                 UUID PRIMARY KEY  DEFAULT uuidv7(),
    type               VARCHAR(10)       NOT NULL,
    status             VARCHAR(15)       NOT NULL,
    slot_start         TIMESTAMP WITH TIME ZONE NOT NULL,
    customer_id        UUID,
    vehicle_id         UUID,
    guest_name         VARCHAR(100),
    guest_phone        VARCHAR(30),
    guest_email        VARCHAR(255),
    guest_vehicle_make VARCHAR(50),
    guest_vehicle_model VARCHAR(100),
    guest_vehicle_year INTEGER,
    complaint          VARCHAR(2000),
    work_order_id      UUID,
    cancel_reason      VARCHAR(20),
    cancel_message     VARCHAR(500),
    rescheduled_to_id  UUID,
    checked_in_at      TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_appointment_type_is_valid CHECK (type IN ('DROPOFF', 'PICKUP')),
    CONSTRAINT chk_appointment_status_is_valid CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_appointment_cancel_reason_is_valid CHECK (
        cancel_reason IS NULL OR cancel_reason IN ('CUSTOMER_REQUESTED', 'STAFF_REQUESTED', 'RESCHEDULED', 'MANAGER_CLOSURE')
        )
);

CREATE INDEX idx_appointments_type_slot_status ON scheduling.appointments (type, slot_start, status);
CREATE INDEX idx_appointments_customer ON scheduling.appointments (customer_id);
CREATE INDEX idx_appointments_work_order ON scheduling.appointments (work_order_id);

CREATE TABLE scheduling.appointment_access_tokens
(
    id             UUID PRIMARY KEY DEFAULT uuidv7(),
    appointment_id UUID                     NOT NULL,
    token_hash     VARCHAR(64)              NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_access_tokens_on_appointments FOREIGN KEY (appointment_id) REFERENCES scheduling.appointments (id)
);

CREATE UNIQUE INDEX idx_access_tokens_hash ON scheduling.appointment_access_tokens (token_hash);

CREATE TABLE scheduling.appointment_registration_tokens
(
    id             UUID PRIMARY KEY DEFAULT uuidv7(),
    appointment_id UUID                     NOT NULL,
    token_hash     VARCHAR(64)              NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at        TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_registration_tokens_on_appointments FOREIGN KEY (appointment_id) REFERENCES scheduling.appointments (id)
);

CREATE UNIQUE INDEX idx_registration_tokens_hash ON scheduling.appointment_registration_tokens (token_hash);

CREATE TABLE scheduling.pickup_invitation_tokens
(
    id           UUID PRIMARY KEY DEFAULT uuidv7(),
    work_order_id UUID                     NOT NULL,
    customer_id  UUID                     NOT NULL,
    vehicle_id   UUID                     NOT NULL,
    token_hash   VARCHAR(64)              NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_pickup_invitation_tokens_hash ON scheduling.pickup_invitation_tokens (token_hash);

CREATE TABLE scheduling.scheduling_settings
(
    id                    UUID PRIMARY KEY DEFAULT uuidv7(),
    business_start_time   TIME                     NOT NULL,
    business_end_time     TIME                     NOT NULL,
    dropoff_slot_capacity INTEGER                  NOT NULL CHECK (dropoff_slot_capacity > 0),
    pickup_slot_capacity  INTEGER                  NOT NULL CHECK (pickup_slot_capacity > 0)
);

INSERT INTO scheduling.scheduling_settings (id, business_start_time, business_end_time, dropoff_slot_capacity, pickup_slot_capacity)
VALUES (uuidv7(), '08:00', '18:00', 3, 3);

CREATE TABLE scheduling.closures
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    date       DATE                     NOT NULL UNIQUE,
    message    VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
