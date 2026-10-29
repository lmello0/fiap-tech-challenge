variable "ministack_endpoint" {
  description = "MiniStack AWS endpoint (output of 01-foundation)."
  type        = string
  default     = "http://localhost:4566"
}

variable "kubeconfig_path" {
  description = "kubeconfig that contains the cluster context."
  type        = string
  default     = "~/.kube/config"
}

variable "kube_context" {
  description = "kubectl context of the cluster (output of 01-foundation)."
  type        = string
  default     = "workshop"
}

variable "namespace" {
  description = "Namespace that receives the application."
  type        = string
  default     = "workshop"
}

variable "database_host" {
  description = "Name under which the cluster reaches the database published on the host."
  type        = string
  default     = "host.minikube.internal"
}

variable "realm_file" {
  description = "Keycloak realm imported at startup."
  type        = string
  default     = "../../config/keycloak/realm-workshop.json"
}
