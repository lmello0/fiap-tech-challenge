resource "docker_image" "ministack" {
  name         = var.ministack_image
  keep_locally = true
}

resource "docker_container" "ministack" {
  name    = "workshop-ministack"
  image   = docker_image.ministack.image_id
  restart = "unless-stopped"
  wait    = true

  ports {
    internal = 4566
    external = var.ministack_port
  }

  volumes {
    host_path      = "/var/run/docker.sock"
    container_path = "/var/run/docker.sock"
  }

  env = [
    "RDS_BASE_PORT=${var.rds_base_port}",
    "MINISTACK_RDS_PUBLIC_ENDPOINT=1",
  ]

  healthcheck {
    test     = ["CMD", "python", "-c", "import urllib.request;urllib.request.urlopen('http://localhost:4566/_ministack/health')"]
    interval = "2s"
    timeout  = "3s"
    retries  = 30
  }
}

resource "minikube_cluster" "workshop" {
  cluster_name = var.cluster_name
  driver       = "docker"
  cpus         = var.cluster_cpus
  memory       = var.cluster_memory

  addons = [
    "default-storageclass",
    "storage-provisioner",
    "metrics-server",
  ]
}
