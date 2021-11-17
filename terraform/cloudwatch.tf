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
  namespace                 = "SuspensionService"
  alarm_description         = "Alarm to flag failed health checks"
  statistic                 = "Maximum"
  treat_missing_data        = "breaching"
  period                    = "60"
  dimensions = {
    "Environment" = var.environment
  }
}
