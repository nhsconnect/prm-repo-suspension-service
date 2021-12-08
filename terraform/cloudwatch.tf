locals {
  error_logs_metric_name              = "ErrorCountInLogs"
  suspension_service_metric_namespace = "SuspensionService"
  not_suspended_sns_topic_name        = "${var.environment}-suspension-service-not-suspended-sns-topic"
  sns_topic_namespace = "AWS/SNS"
  sqs_namespace = "AWS/SQS"
  sns_topic_error_logs_metric_name = "NumberOfNotificationsFailed"
}

resource "aws_cloudwatch_log_group" "log_group" {
  name = "/nhs/deductions/${var.environment}-${data.aws_caller_identity.current.account_id}/${var.component_name}"

  tags = {
    Environment = var.environment
    CreatedBy= var.repo_name
  }
}

resource "aws_cloudwatch_metric_alarm" "health_metric_failure_alarm" {
  alarm_name                = "${var.component_name}-health-metric-failure"
  comparison_operator       = "LessThanThreshold"
  threshold                 = "1"
  evaluation_periods        = "3"
  metric_name               = "Health"
  namespace                 = local.suspension_service_metric_namespace
  alarm_description         = "Alarm to flag failed health checks"
  statistic                 = "Maximum"
  treat_missing_data        = "breaching"
  period                    = "60"
  dimensions = {
    "Environment" = var.environment
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
  alarm_name                = "${var.environment}-${var.component_name}-error-logs"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "0"
  evaluation_periods        = "1"
  period                    = "60"
  metric_name               = local.error_logs_metric_name
  namespace                 = local.suspension_service_metric_namespace
  statistic                 = "Sum"
  alarm_description         = "This alarm monitors errors logs in ${var.component_name}"
  treat_missing_data        = "notBreaching"
  actions_enabled           = "true"
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

data "aws_sns_topic" "alarm_notifications" {
  name = "${var.environment}-alarm-notifications-sns-topic"
}

resource "aws_cloudwatch_metric_alarm" "not_suspended_sns_topic_error_log_alarm" {
  alarm_name                = "${local.not_suspended_sns_topic_name}-error-logs"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "0"
  evaluation_periods        = "1"
  period                    = "60"
  metric_name               = local.sns_topic_error_logs_metric_name
  namespace                 = local.sns_topic_namespace
  dimensions = {
    TopicName = local.not_suspended_sns_topic_name
  }
  statistic                 = "Sum"
  alarm_description         = "This alarm monitors errors logs in ${local.not_suspended_sns_topic_name}"
  treat_missing_data        = "notBreaching"
  actions_enabled           = "true"
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspensions_queue_ratio_of_received_to_acknowledgement" {
  alarm_name                = "${var.environment}-suspensions-queue-ratio-of-received-to-acknowledgement"
  comparison_operator       = "LessThanOrEqualToThreshold"
  evaluation_periods        = "1"
  threshold                 = "90"
  alarm_description         = "Received message ratio to acknowledgement exceeds %20"
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]

  metric_query {
    id          = "e1"
    expression  = "(acknowledgement+0.1)/(received+0.1)*100"
    label       = "Expression"
    return_data = "true"
  }

  metric_query {
    id = "received"

    metric {
      metric_name = "NumberOfMessagesReceived"
      namespace   = local.sqs_namespace
      period      = "300"
      stat        = "Sum"
      unit        = "Count"

      dimensions = {
        QueueName = aws_sqs_queue.suspensions.name
      }
    }
  }

  metric_query {
    id = "acknowledgement"

    metric {
      metric_name = "NumberOfMessagesDeleted"
      namespace   = local.sqs_namespace
      period      = "300"
      stat        = "Sum"
      unit        = "Count"

      dimensions = {
        QueueName = aws_sqs_queue.suspensions.name
      }
    }
  }
}
