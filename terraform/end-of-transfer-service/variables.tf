variable "region" {
  type    = string
  default = "eu-west-2"
}

variable "repo_name" {
  type = string
}

variable "environment" {}

variable "component_name" {}

variable "ecs_desired_count" {
  default = 1
}