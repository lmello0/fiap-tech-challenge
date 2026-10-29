data "aws_secretsmanager_secret_version" "app_db" {
  secret_id = "workshop/db/app"
}

data "aws_secretsmanager_secret_version" "keycloak_db" {
  secret_id = "workshop/db/keycloak"
}

locals {
  app_db      = jsondecode(data.aws_secretsmanager_secret_version.app_db.secret_string)
  keycloak_db = jsondecode(data.aws_secretsmanager_secret_version.keycloak_db.secret_string)
}

resource "random_password" "keycloak_admin" {
  length  = 24
  special = false
}

resource "random_password" "keycloak_admin_client" {
  length  = 32
  special = false
}

resource "random_password" "budget_webhook" {
  length  = 40
  special = false
}

resource "kubernetes_namespace_v1" "workshop" {
  metadata {
    name = var.namespace
  }
}

resource "kubernetes_secret_v1" "workshop_db" {
  metadata {
    name      = "workshop-db"
    namespace = kubernetes_namespace_v1.workshop.metadata[0].name
  }

  data = {
    DB_URL      = "jdbc:postgresql://${var.database_host}:${local.app_db.port}/${local.app_db.dbname}"
    DB_USER     = local.app_db.username
    DB_PASSWORD = local.app_db.password
  }
}

resource "kubernetes_secret_v1" "keycloak_db" {
  metadata {
    name      = "keycloak-db"
    namespace = kubernetes_namespace_v1.workshop.metadata[0].name
  }

  data = {
    KC_DB_URL      = "jdbc:postgresql://${var.database_host}:${local.keycloak_db.port}/${local.keycloak_db.dbname}"
    KC_DB_USERNAME = local.keycloak_db.username
    KC_DB_PASSWORD = local.keycloak_db.password
  }
}

resource "kubernetes_secret_v1" "keycloak_bootstrap_admin" {
  metadata {
    name      = "keycloak-bootstrap-admin"
    namespace = kubernetes_namespace_v1.workshop.metadata[0].name
  }

  data = {
    KC_BOOTSTRAP_ADMIN_USERNAME = "admin"
    KC_BOOTSTRAP_ADMIN_PASSWORD = random_password.keycloak_admin.result
  }
}

resource "kubernetes_secret_v1" "keycloak_admin_client" {
  metadata {
    name      = "keycloak-admin-client"
    namespace = kubernetes_namespace_v1.workshop.metadata[0].name
  }

  data = {
    KEYCLOAK_ADMIN_CLIENT_SECRET = random_password.keycloak_admin_client.result
  }
}

resource "kubernetes_secret_v1" "budget_webhook" {
  metadata {
    name      = "budget-webhook"
    namespace = kubernetes_namespace_v1.workshop.metadata[0].name
  }

  data = {
    BUDGET_WEBHOOK_SECRET = random_password.budget_webhook.result
  }
}

resource "kubernetes_secret_v1" "keycloak_realm" {
  metadata {
    name      = "keycloak-realm"
    namespace = kubernetes_namespace_v1.workshop.metadata[0].name
  }

  data = {
    "realm-workshop.json" = file("${path.module}/${var.realm_file}")
  }
}
