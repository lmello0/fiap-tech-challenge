resource "random_password" "master" {
  length  = 24
  special = false
}

resource "random_password" "app" {
  length  = 24
  special = false
}

resource "random_password" "keycloak" {
  length  = 24
  special = false
}

resource "aws_db_instance" "postgres" {
  identifier          = "workshop"
  engine              = "postgres"
  engine_version      = var.engine_version
  instance_class      = "db.t3.micro"
  allocated_storage   = 20
  username            = "postgres"
  password            = random_password.master.result
  publicly_accessible = true
  skip_final_snapshot = true
}

resource "postgresql_role" "app" {
  name     = "workshop_app"
  login    = true
  password = random_password.app.result
}

resource "postgresql_database" "workshop" {
  name  = "workshop"
  owner = postgresql_role.app.name
}

resource "postgresql_role" "keycloak" {
  name     = "keycloak"
  login    = true
  password = random_password.keycloak.result
}

resource "postgresql_database" "keycloak" {
  name  = "keycloak"
  owner = postgresql_role.keycloak.name
}

resource "aws_secretsmanager_secret" "app" {
  name                    = "workshop/db/app"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "app" {
  secret_id = aws_secretsmanager_secret.app.id
  secret_string = jsonencode({
    port     = aws_db_instance.postgres.port
    dbname   = postgresql_database.workshop.name
    username = postgresql_role.app.name
    password = random_password.app.result
  })
}

resource "aws_secretsmanager_secret" "keycloak" {
  name                    = "workshop/db/keycloak"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "keycloak" {
  secret_id = aws_secretsmanager_secret.keycloak.id
  secret_string = jsonencode({
    port     = aws_db_instance.postgres.port
    dbname   = postgresql_database.keycloak.name
    username = postgresql_role.keycloak.name
    password = random_password.keycloak.result
  })
}
