output "database_port" {
  description = "Host port where the RDS instance is published."
  value       = aws_db_instance.postgres.port
}

output "database_version" {
  value = aws_db_instance.postgres.engine_version
}
