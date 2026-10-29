terraform {
  required_version = ">= 1.9"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.6"
    }
    minikube = {
      source  = "scott-the-programmer/minikube"
      version = "~> 0.8"
    }
  }
}

provider "docker" {
  host = var.docker_host
}
