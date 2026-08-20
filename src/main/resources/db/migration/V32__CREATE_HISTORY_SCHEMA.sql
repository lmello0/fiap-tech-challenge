-- History (ADR 0011/0012): an append-only, query-only timeline. Every row is written by reacting to
-- a DomainEvent; nothing — including a direct psql session — may rewrite or remove one afterward.

CREATE SCHEMA IF NOT EXISTS history;

CREATE TABLE history.entry
(
    id               UUID PRIMARY KEY        DEFAULT uuidv7(),
    aggregate_type   VARCHAR(40)     NOT NULL,
    aggregate_id     UUID            NOT NULL,
    entity_type      VARCHAR(40)     NOT NULL,
    entity_id        UUID            NOT NULL,
    event_type       VARCHAR(60)     NOT NULL,
    occurred_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    actor_type       VARCHAR(10)     NOT NULL CHECK (actor_type IN ('USER', 'SYSTEM')),
    actor_id         UUID,
    actor_label      VARCHAR(100),
    customer_visible BOOLEAN         NOT NULL DEFAULT false,
    schema_version   INT             NOT NULL DEFAULT 1,
    snapshot         JSONB           NOT NULL
);

CREATE INDEX idx_entry_aggregate_occurred_at
    ON history.entry (aggregate_type, aggregate_id, occurred_at DESC);

CREATE OR REPLACE FUNCTION history.forbid_entry_mutation() RETURNS trigger AS
$$
BEGIN
    RAISE EXCEPTION 'history.entry rows are append-only and cannot be % (id=%)', TG_OP, OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_entry_append_only
    BEFORE UPDATE OR DELETE
    ON history.entry
    FOR EACH ROW
EXECUTE FUNCTION history.forbid_entry_mutation();
