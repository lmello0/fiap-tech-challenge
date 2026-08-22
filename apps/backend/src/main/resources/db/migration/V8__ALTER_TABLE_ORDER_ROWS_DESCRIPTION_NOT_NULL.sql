ALTER TABLE work_orders.order_rows
ALTER COLUMN description SET NOT NULL;

ALTER TABLE work_orders.order_rows
DROP CONSTRAINT order_rows_type_check CASCADE;

ALTER TABLE work_orders.order_rows
ADD CONSTRAINT order_rows_type_check
CHECK (type in ('LABOR', 'PART'))