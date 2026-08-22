-- ADR 0013: Budget becomes the owning side of its relationship to WorkOrder. WorkOrder no longer
-- holds a redundant budget_id column; budgets.work_order_id (already FK'd since V30) is the single
-- foreign key. Cardinality ("exactly one live Budget per WorkOrder") stays an application rule, not a
-- database constraint, so requoting can be added later without a migration.

ALTER TABLE work_orders.orders
    DROP CONSTRAINT IF EXISTS fk_orders_on_budgets;

ALTER TABLE work_orders.orders
    DROP COLUMN IF EXISTS budget_id;
