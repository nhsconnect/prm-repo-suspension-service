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
  default = 512
}
variable "task_memory" {
  default = 1024
}

variable "log_level" {
  type = string
  default = "debug"
}

//this might change per environment.
variable "threshold_for_suspensions_queue_age_of_message" {
  default = "86400"
}

variable "period_of_age_of_message_metric" {
  default = "1800"
}

variable "process_only_synthetic_patients" {
  default = true
}

variable "synthetic_patient_prefix" {}

variable "scale_up_expression" {
  type    = string
  default = "((HOUR(m1)==12 && MINUTE(m1)==30 )),10,0"
}

variable "enable_scale_action" {
  type    = bool
  default = true
}

variable "scale_down_number_of_empty_receives_count" {
  type    = string
  default = 15
}

variable "core_task_number" {
  default = ""
}
