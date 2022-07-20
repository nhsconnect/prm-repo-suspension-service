variable "region" {
  type    = string
  default = "eu-west-2"
}

variable "repo_name" {
  type = string
}

variable "environment" {}

variable "component_name" {}

variable "metric_namespace" {}

variable "environment_dns_zone" {
  description = "The environment-specific labels of the dns zone name, e.g. 'prod' or 'dev.non-prod'"
}

variable "task_image_tag" {}

variable "task_cpu" {
  default = 512
}
variable "task_memory" {
  default = 1024
}

variable "log_level" {
  type = string
  default = "debug"
}

variable "process_only_synthetic_or_safe_listed_patients" {
  default = true
}

variable "synthetic_patient_prefix" {}

variable "scale_up_expression" {
  type    = string
  default = "((HOUR(m1)==21 && MINUTE(m1)==58 )),10,0"
}

variable "enable_scale_action" {
  type    = bool
  default = true
}

variable "can_update_managing_organisation_to_repo" {
  description = "Toggle to allow updating managing organisation to repo ODS code"
  default = false
}

variable "is_end_of_transfer_service" {
  type = bool
  default = false
}

variable "image_name" {
  type = string
  default = "suspension-service"
}

variable "ecs_desired_count" {
  default = 0
}
