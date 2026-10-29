variable "ministack_endpoint" {
  description = "MiniStack AWS endpoint (output of 01-foundation)."
  type        = string
  default     = "http://localhost:4566"
}

variable "engine_version" {
  description = "PostgreSQL engine version (MiniStack runs the matching postgres image)."
  type        = string
  default     = "18.1"
}
