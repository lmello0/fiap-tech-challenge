-- A reservation fully released (PartReservationServiceImpl#releasePartial draining it completely,
-- e.g. BudgetServiceImpl#removeLine removing a PART line outright) legitimately settles at
-- quantity_requested = 0 while its status moves to RELEASED — the row is kept as history, not
-- deleted. The original "> 0" check made that unreachable, throwing a DataIntegrityViolationException
-- from a live code path.
ALTER TABLE inventory.part_reservations
    DROP CONSTRAINT part_reservations_quantity_requested_check;

ALTER TABLE inventory.part_reservations
    ADD CONSTRAINT chk_quantity_requested_not_negative CHECK (quantity_requested >= 0);
