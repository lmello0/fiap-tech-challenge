variable "docker_host" {
  description = "Docker daemon socket used to run MiniStack."
  type        = string
  default     = "unix:///var/run/docker.sock"
}

variable "ministack_image" {
  description = "MiniStack image (AWS emulator)."
  type        = string
  default     = "ministackorg/ministack:1.5.31"
}

variable "ministack_port" {
  description = "Host port where the MiniStack AWS endpoint listens."
  type        = number
  default     = 4566
}

variable "rds_base_port" {
  description = "First host port MiniStack publishes RDS instances on."
  type        = number
  default     = 15432
}

variable "cluster_name" {
  description = "minikube profile and kubectl context name."
  type        = string
  default     = "workshop"
}

variable "cluster_cpus" {
  description = "CPUs given to the minikube node."
  type        = number
  default     = 4
}

variable "cluster_memory" {
  description = "Memory given to the minikube node."
  type        = string
  default     = "8192mb"
}
