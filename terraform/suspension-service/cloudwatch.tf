locals {
  sqs_namespace                       = "AWS/SQS"
}

module "shared-cloudwatch" {
  source = "../shared"
  environment = var.environment
  component_name = var.component_name
  repo_name = var.repo_name
  account_id = data.aws_caller_identity.current.account_id
  alarm_notifications_arn = data.aws_sns_topic.alarm_notifications.arn
  event_out_of_order_audit_queue_name = aws_sqs_queue.event_out_of_order_audit.name
  not_suspended_audit_queue_name = aws_sqs_queue.not_suspended_audit.name
  mof_not_updated_audit_queue_name = aws_sqs_queue.mof_not_updated_audit.name
  mof_updated_audit_queue_name = aws_sqs_queue.mof_updated_audit.name
  deceased_patient_audit_queue_name = aws_sqs_queue.deceased_patient_audit.name
  invalid_suspension_audit_queue_name = aws_sqs_queue.invalid_suspension_audit.name
}

#moved {
#  from = aws_cloudwatch_log_group.log_group
#  to = module.shared_cloudwatch.aws_cloudwatch_log_group.log_group
#}
#
#moved {
#  from = aws_cloudwatch_log_metric_filter.log_metric_filter
#  to = module.shared_cloudwatch.aws_cloudwatch_log_metric_filter.log_metric_filter
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.error_log_alarm
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.error_log_alarm
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.not_suspended_sns_topic_error_log_alarm
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.not_suspended_sns_topic_error_log_alarm
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_out_of_order_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_out_of_order_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_not_suspended_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_not_suspended_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_mof_not_updated_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_mof_not_updated_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_mof_updated_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_mof_updated_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_deceased_patient_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_deceased_patient_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_invalid_suspension_dlq_audit
#  to   = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_invalid_suspension_dlq_audit
#}

resource "aws_cloudwatch_metric_alarm" "suspension_service_scale_up_alarm" {
  alarm_name          = "${var.environment}-${var.component_name}-scale-up"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  threshold           = "10"
  alarm_description   = "Scale up alarm for suspension-service"
  actions_enabled     = true
  alarm_actions       = [aws_appautoscaling_policy.scale_up.arn]

  metric_query {
    id          = "e1"
    expression  = "IF (${var.scale_up_expression})"
    label       = "Expression"
    return_data = "true"
  }

  metric_query {
    id = "m1"

    metric {
      metric_name = "NumberOfMessagesReceived"
      namespace   = local.sqs_namespace
      period      = "180"
      stat        = "Sum"
      unit        = "Count"

      dimensions = {
        QueueName = aws_sqs_queue.suspensions.name
      }
    }
  }
}

resource "aws_cloudwatch_metric_alarm" "suspension_service_scale_down_alarm" {
  alarm_name          = "${var.environment}-${var.component_name}-scale-down"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = var.scale_down_number_of_empty_receives_count * var.core_task_number
  evaluation_periods  = "1"
  metric_name         = "NumberOfEmptyReceives"
  namespace           = local.sqs_namespace
  alarm_description   = "Alarm to alert when all events are processed in the queue"
  statistic           = "Sum"
  period              = 300
  dimensions          = {
    QueueName = aws_sqs_queue.suspensions.name
  }
  treat_missing_data  = "notBreaching"
  actions_enabled     = var.enable_scale_action
  alarm_actions       = [aws_appautoscaling_policy.scale_down.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspensions_queue_age_of_message" {
  alarm_name          = "${var.environment}-${var.component_name}-queue-age-of-message"
  comparison_operator = "GreaterThanThreshold"
  threshold           = var.threshold_for_suspensions_queue_age_of_message
  evaluation_periods  = "1"
  metric_name         = "ApproximateAgeOfOldestMessage"
  namespace           = local.sqs_namespace
  alarm_description   = "Alarm to alert approximate time for message in the queue"
  statistic           = "Maximum"
  period              = var.period_of_age_of_message_metric
  dimensions          = {
    QueueName = aws_sqs_queue.suspensions.name
  }
  alarm_actions       = [data.aws_sns_topic.alarm_notifications.arn]
}