# TODO: add validation to make all of them mandatory
variable "environment" {}
variable "component_name" {}
variable "repo_name" {}
variable "account_id" {}
variable "alarm_notifications_arn" {}
variable "event_out_of_order_audit_queue_name" {}
variable "not_suspended_audit_queue_name" {}
variable "mof_not_updated_audit_queue_name" {}
variable "mof_updated_audit_queue_name" {}
variable "deceased_patient_audit_queue_name" {}
variable "invalid_suspension_audit_queue_name" {}

locals {
  error_logs_metric_name              = "ErrorCountInLogs"
  suspension_service_metric_namespace = "SuspensionService"
  not_suspended_sns_topic_name        = "${var.environment}-${var.component_name}-not-suspended-sns-topic"
  sns_topic_namespace                 = "AWS/SNS"
  sqs_namespace                       = "AWS/SQS"
  sns_topic_error_logs_metric_name    = "NumberOfNotificationsFailed"
}

resource "aws_cloudwatch_log_group" "log_group" {
  name = "/nhs/deductions/${var.environment}-${var.account_id}/${var.component_name}"

  tags = {
    Environment = var.environment
    CreatedBy   = var.repo_name
  }
}

resource "aws_cloudwatch_log_metric_filter" "log_metric_filter" {
  name           = "${var.environment}-${var.component_name}-error-logs"
  pattern        = "{ $.level = \"ERROR\" }"
  log_group_name = aws_cloudwatch_log_group.log_group.name

  metric_transformation {
    name          = local.error_logs_metric_name
    namespace     = local.suspension_service_metric_namespace
    value         = 1
    default_value = 0
  }
}

resource "aws_cloudwatch_metric_alarm" "error_log_alarm" {
  alarm_name          = "${var.environment}-${var.component_name}-error-logs"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "0"
  evaluation_periods  = "1"
  period              = "60"
  metric_name         = local.error_logs_metric_name
  namespace           = local.suspension_service_metric_namespace
  statistic           = "Sum"
  alarm_description   = "This alarm monitors errors logs in ${var.component_name}"
  treat_missing_data  = "notBreaching"
  actions_enabled     = "true"
  alarm_actions       = [var.alarm_notifications_arn]
}

resource "aws_cloudwatch_metric_alarm" "not_suspended_sns_topic_error_log_alarm" {
  alarm_name          = "${local.not_suspended_sns_topic_name}-error-logs"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "0"
  evaluation_periods  = "1"
  period              = "60"
  metric_name         = local.sns_topic_error_logs_metric_name
  namespace           = local.sns_topic_namespace
  dimensions          = {
    TopicName = local.not_suspended_sns_topic_name
  }
  statistic           = "Sum"
  alarm_description   = "This alarm monitors errors logs in ${local.not_suspended_sns_topic_name}"
  treat_missing_data  = "notBreaching"
  actions_enabled     = "true"
  alarm_actions       = [var.alarm_notifications_arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_out_of_order_audit" {
  alarm_name          = "${var.environment}-${var.component_name}-out-of-order-audit"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "900"
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = local.sqs_namespace
  alarm_description   = "This alarm triggers when messages on the out of order audit queue is not polled by splunk in last 15 mins"
  statistic           = "Maximum"
  period              = "900"
  dimensions          = {
    QueueName = var.event_out_of_order_audit_queue_name
  }
  alarm_actions       = [var.alarm_notifications_arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_not_suspended_audit" {
  alarm_name          = "${var.environment}-${var.component_name}-not-suspended-audit"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "900"
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = local.sqs_namespace
  alarm_description   = "This alarm triggers when messages on the not suspended audit queue is not polled by splunk in last 15 mins"
  statistic           = "Maximum"
  period              = "900"
  dimensions          = {
    QueueName = var.not_suspended_audit_queue_name
  }
  alarm_actions       = [var.alarm_notifications_arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_mof_not_updated_audit" {
  alarm_name          = "${var.environment}-${var.component_name}-mof-not-updated-audit"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "900"
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = local.sqs_namespace
  alarm_description   = "This alarm triggers when messages on the MOF not updated audit queue is not polled by splunk in last 15 mins"
  statistic           = "Maximum"
  period              = "900"
  dimensions          = {
    QueueName = var.mof_not_updated_audit_queue_name
  }
  alarm_actions       = [var.alarm_notifications_arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_mof_updated_audit" {
  alarm_name          = "${var.environment}-${var.component_name}-mof-updated-audit"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "900"
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = local.sqs_namespace
  alarm_description   = "This alarm triggers when messages on the MOF updated audit queue is not polled by splunk in last 15 mins"
  statistic           = "Maximum"
  period              = "900"
  dimensions          = {
    QueueName = var.mof_updated_audit_queue_name
  }
  alarm_actions       = [var.alarm_notifications_arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_deceased_patient_audit" {
  alarm_name          = "${var.environment}-${var.component_name}-deceased-patient-audit"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "900"
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = local.sqs_namespace
  alarm_description   = "This alarm triggers when messages on the deceased patient audit queue is not polled by splunk in last 15 mins"
  statistic           = "Maximum"
  period              = "900"
  dimensions          = {
    QueueName = var.deceased_patient_audit_queue_name
  }
  alarm_actions       = [var.alarm_notifications_arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_invalid_suspension_dlq_audit" {
  alarm_name          = "${var.environment}-${var.component_name}-invalid-suspension-dlq-audit"
  comparison_operator = "GreaterThanThreshold"
  threshold           = "900"
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = local.sqs_namespace
  alarm_description   = "This alarm triggers when messages on the invalid suspensions dlq audit queue is not polled by splunk in last 15 mins"
  statistic           = "Maximum"
  period              = "900"
  dimensions          = {
    QueueName = var.invalid_suspension_audit_queue_name
  }
  alarm_actions       = [var.alarm_notifications_arn]
}