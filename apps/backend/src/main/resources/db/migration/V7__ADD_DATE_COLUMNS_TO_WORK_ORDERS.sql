ALTER TABLE work_orders.orders
    ADD COLUMN diagnostic_requested_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN diagnostic_started_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN service_started_at      TIMESTAMP WITH TIME ZONE,
    ADD COLUMN pickup_ready_at         TIMESTAMP WITH TIME ZONE
;