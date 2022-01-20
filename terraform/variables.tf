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

variable "service_desired_count" {
  default = 1
}

variable "log_level" {
  type = string
  default = "debug"
}

variable "grant_access_through_vpn" {}

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

variable "suspension_service_start_schedule_expression" {
  # TODO: what will be the time for testprod?
  default = "cron(0 6 * * *)"
}