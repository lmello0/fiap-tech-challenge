ALTER TABLE users.workers
    DROP CONSTRAINT workers_role_check,
    ADD CONSTRAINT chk_worker_role_valid CHECK (role IN ('MECHANIC', 'ATTENDANT', 'MANAGER', 'STOCKIST'));
