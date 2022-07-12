variable "region" {
  type    = string
  default = "eu-west-2"
}

variable "repo_name" {
  type = string
}

variable "environment" {}

variable "component_name" {}

variable "sns_sqs_role_arn" {}
