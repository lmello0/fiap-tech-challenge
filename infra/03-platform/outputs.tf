output "namespace" {
  value = kubernetes_namespace_v1.workshop.metadata[0].name
}

output "budget_webhook_secret" {
  description = "HMAC secret of the budget decision webhook (use it to sign test requests)."
  value       = random_password.budget_webhook.result
  sensitive   = true
}

output "keycloak_admin_password" {
  description = "Password of the Keycloak bootstrap administrator."
  value       = random_password.keycloak_admin.result
  sensitive   = true
}
