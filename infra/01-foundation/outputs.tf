output "ministack_endpoint" {
  description = "AWS endpoint (MiniStack) used by the next stage."
  value       = "http://localhost:${var.ministack_port}"
}

output "rds_base_port" {
  value = var.rds_base_port
}

output "kube_context" {
  description = "kubectl context of the cluster."
  value       = var.cluster_name
}
