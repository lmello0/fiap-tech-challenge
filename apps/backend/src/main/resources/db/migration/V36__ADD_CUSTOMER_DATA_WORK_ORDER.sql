ALTER TABLE work_orders.orders
    ADD COLUMN customer_name VARCHAR(60),
    ADD COLUMN vehicle_plate VARCHAR(7),
    ADD COLUMN vehicle_make  VARCHAR(50),
    ADD COLUMN vehicle_model VARCHAR(100),
    ADD COLUMN mechanic_name VARCHAR(60);