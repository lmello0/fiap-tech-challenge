terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    postgresql = {
      source  = "cyrilgdn/postgresql"
      version = "~> 1.27"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7"
    }
  }
}

provider "aws" {
  region     = "us-east-1"
  access_key = "test"
  secret_key = "test"

  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    rds            = var.ministack_endpoint
    secretsmanager = var.ministack_endpoint
  }
}

provider "postgresql" {
  host      = aws_db_instance.postgres.address
  port      = aws_db_instance.postgres.port
  username  = aws_db_instance.postgres.username
  password  = random_password.master.result
  sslmode   = "disable"
  superuser = true
}
