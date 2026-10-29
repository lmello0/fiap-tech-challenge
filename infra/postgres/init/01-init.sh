#!/bin/sh
# Creates the single application role/database and a separate role/database for Keycloak.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE ROLE workshop_app LOGIN PASSWORD '${WORKSHOP_DB_PASSWORD}';
CREATE DATABASE workshop OWNER workshop_app;
CREATE ROLE keycloak LOGIN PASSWORD '${KEYCLOAK_DB_PASSWORD}';
CREATE DATABASE keycloak OWNER keycloak;
SQL
