CREATE SCHEMA vehicle;

CREATE TABLE IF NOT EXISTS vehicle.vehicles
(
    id                UUID PRIMARY KEY      DEFAULT uuidv7(),
    customer_id       UUID         NOT NULL,
    vehicle_type      VARCHAR(30)  NOT NULL,
    license_plate     VARCHAR(7)   NOT NULL,
    vin               VARCHAR(17),
    make              VARCHAR(50)  NOT NULL,
    model             VARCHAR(100) NOT NULL,
    color             VARCHAR(30)  NOT NULL,
    model_year        int          NOT NULL,
    manufacture_year  int,
    version           VARCHAR(100),
    fuel_type         VARCHAR(10)  NOT NULL,
    transmission_type VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ,

    CONSTRAINT fk_vehicles_on_users
        FOREIGN KEY (customer_id)
            REFERENCES users.users (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_vehicles_license_plate UNIQUE (vehicle_type),

    CONSTRAINT uk_vehicles_vin UNIQUE (vin),

    CONSTRAINT chk_vehicle_type_in CHECK (
        vehicle_type IN (
                         'CAR',
                         'MOTORCYCLE',
                         'TRUCK',
                         'VAN',
                         'SUV',
                         'BUS',
                         'OTHER'
            )
        ),

    CONSTRAINT chk_fuel_type_in CHECK ( fuel_type in (
                                                      'GASOLINE',
                                                      'ETHANOL',
                                                      'FLEX',
                                                      'DIESEL',
                                                      'ELECTRIC',
                                                      'GNV',
                                                      'HYBRID',
                                                      'OTHER'
        )
        )
);