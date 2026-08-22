ALTER TABLE users.workers
    ADD COLUMN start_date DATE NOT NULL;

ALTER TABLE users.workers
    DROP CONSTRAINT chk_termination_date_is_greater_than_hire_date,
    ADD CONSTRAINT chk_contract_dates_are_valid CHECK (
        (termination_date IS NULL OR termination_date >= hire_date)
            AND start_date >= hire_date
            AND start_date < termination_date
        );