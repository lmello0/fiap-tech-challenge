CREATE TABLE processed_event (
    event_id     UUID                     NOT NULL,
    listener     VARCHAR(100)             NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, listener)
);
