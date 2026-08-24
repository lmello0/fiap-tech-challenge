ALTER TABLE work_orders.orders
DROP COLUMN assigned_mechanic_name;

UPDATE work_orders.orders o
SET
    mechanic_name = (
        SELECT u.first_name || ' ' || u.last_name
        FROM users.users u
        WHERE u.id = o.assigned_mechanic_id
    );