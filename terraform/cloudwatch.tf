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
  threshold                 = "80"
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

resource "aws_cloudwatch_metric_alarm" "suspension_out_of_order_audit" {
  alarm_name                = "${var.environment}-${var.component_name}-out-of-order-audit"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "900"
  evaluation_periods        = "1"
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = local.sqs_namespace
  alarm_description         = "This alarm triggers when messages on the out of order audit queue is not polled by splunk in last 15 mins"
  statistic                 = "Maximum"
  period                    = "900"
  dimensions = {
    QueueName = aws_sqs_queue.event_out_of_order_audit.name
  }
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_not_suspended_audit" {
  alarm_name                = "${var.environment}-${var.component_name}-not-suspended-audit"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "900"
  evaluation_periods        = "1"
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = local.sqs_namespace
  alarm_description         = "This alarm triggers when messages on the not suspended audit queue is not polled by splunk in last 15 mins"
  statistic                 = "Maximum"
  period                    = "900"
  dimensions = {
    QueueName = aws_sqs_queue.not_suspended_audit.name
  }
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_mof_not_updated_audit" {
  alarm_name                = "${var.environment}-${var.component_name}-mof-not-updated-audit"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "900"
  evaluation_periods        = "1"
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = local.sqs_namespace
  alarm_description         = "This alarm triggers when messages on the MOF not updated audit queue is not polled by splunk in last 15 mins"
  statistic                 = "Maximum"
  period                    = "900"
  dimensions = {
    QueueName = aws_sqs_queue.mof_not_updated_audit.name
  }
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_mof_updated_audit" {
  alarm_name                = "${var.environment}-${var.component_name}-mof-updated-audit"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "900"
  evaluation_periods        = "1"
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = local.sqs_namespace
  alarm_description         = "This alarm triggers when messages on the MOF updated audit queue is not polled by splunk in last 15 mins"
  statistic                 = "Maximum"
  period                    = "900"
  dimensions = {
    QueueName = aws_sqs_queue.mof_updated_audit.name
  }
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_deceased_patient_audit" {
  alarm_name                = "${var.environment}-${var.component_name}-deceased-patient-audit"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "900"
  evaluation_periods        = "1"
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = local.sqs_namespace
  alarm_description         = "This alarm triggers when messages on the deceased patient audit queue is not polled by splunk in last 15 mins"
  statistic                 = "Maximum"
  period                    = "900"
  dimensions = {
    QueueName = aws_sqs_queue.deceased_patient_audit.name
  }
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_invalid_suspension_dlq_audit" {
  alarm_name                = "${var.environment}-${var.component_name}-invalid-suspension-dlq-audit"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 = "900"
  evaluation_periods        = "1"
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = local.sqs_namespace
  alarm_description         = "This alarm triggers when messages on the invalid suspensions dlq audit queue is not polled by splunk in last 15 mins"
  statistic                 = "Maximum"
  period                    = "900"
  dimensions = {
    QueueName = aws_sqs_queue.invalid_suspension_dlq.name
  }
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspensions_queue_age_of_message" {
  alarm_name                = "${var.environment}-${var.component_name}-queue-age-of-message"
  comparison_operator       = "GreaterThanThreshold"
  threshold                 =  var.threshold_for_suspensions_queue_age_of_message
  evaluation_periods        = "1"
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = local.sqs_namespace
  alarm_description         = "Alarm to alert approximate time for message in the queue"
  statistic                 = "Maximum"
  period                    = var.period_of_age_of_message_metric
  dimensions = {
    QueueName = aws_sqs_queue.suspensions.name
  }
  alarm_actions             = [data.aws_sns_topic.alarm_notifications.arn]
}

resource "aws_cloudwatch_metric_alarm" "suspension_service_scale_up_alarm" {
  alarm_name                = "${var.environment}-suspensions-service-scale-up"
  comparison_operator       = "GreaterThanOrEqualToThreshold"
  evaluation_periods        = "1"
  threshold                 = "10"
  alarm_description         = "Scale up alarm for suspension-service"
  actions_enabled           = true
  alarm_actions             = [aws_appautoscaling_policy.scale_up.arn]

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
  alarm_name                = "${var.environment}-${var.component_name}-scale-down"
  comparison_operator       = "GreaterThanOrEqualToThreshold"
  threshold                 =  var.scale_down_number_of_empty_receives_count * var.core_task_number
  evaluation_periods        = "1"
  metric_name               = "NumberOfEmptyReceives"
  namespace                 = local.sqs_namespace
  alarm_description         = "Alarm to alert when all events are processed in the queue"
  statistic                 = "Sum"
  period                    = 300
  dimensions = {
    QueueName = aws_sqs_queue.suspensions.name
  }
  treat_missing_data        = "notBreaching"
  actions_enabled           = var.enable_scale_action
  alarm_actions             = [aws_appautoscaling_policy.scale_down.arn]
}
