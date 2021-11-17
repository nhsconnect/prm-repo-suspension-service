variable "region" {
  type    = string
  default = "eu-west-2"
}

variable "repo_name" {
  type = string
  default = "prm-repo-suspension-service"
}

variable "environment" {}

variable "component_name" {
  default = "suspension-service"
}

variable "dns_name" {
  default = "suspension-service"
}

variable "task_image_tag" {}

variable "task_cpu" {
  default = 256
}
variable "task_memory" {
  default = 512
}

variable "service_desired_count" {
  default = 1
}

variable "log_level" {
  type = string
  default = "debug"
}

variable "grant_access_through_vpn" {}
