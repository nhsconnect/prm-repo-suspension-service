provider "aws" {
  profile   = "default"
  region    = var.region
}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "3.44.0"
    }
  }
}

module "end-of-transfer-service" {
  count = var.is_end_of_transfer_service ? 1 : 0
  source    = "./end-of-transfer-service/"
  environment    = var.environment

  component_name = var.component_name
  repo_name      = var.repo_name
}
