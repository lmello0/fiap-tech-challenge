ALTER TABLE vehicle.vehicles DROP CONSTRAINT uk_vehicles_license_plate;
ALTER TABLE vehicle.vehicles ADD CONSTRAINT uk_vehicles_license_plate UNIQUE (license_plate);

ALTER TABLE vehicle.vehicles ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE vehicle.vehicles ADD COLUMN deactivated_at TIMESTAMPTZ;
