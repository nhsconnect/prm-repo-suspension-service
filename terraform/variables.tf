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

variable "threshold_for_suspensions_queue_age_of_message" {
  description = "An alarm will be raised if any message gets to this many seconds old"
  default = "86400"
}

variable "period_of_age_of_message_metric" {
  default = "1800"
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

variable "scale_down_number_of_empty_receives_count" {
  type    = string
  default = 14
}

variable "core_task_number" {
  description = "Something to do with number of threads used in metric alarm for scale down"
  default = 5
}

variable "can_update_managing_organisation_to_repo" {
  description = "Toggle to allow updating managing organisation to repo ODS code"
  default = false
}
