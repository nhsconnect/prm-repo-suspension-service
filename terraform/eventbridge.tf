resource "aws_cloudwatch_event_rule" "suspension_service_start_event" {
  name        = "${var.environment}-${var.component_name}-start-event"
  description = "Eventbridge (formerly Cloudwatch event) rule to start Suspension Service"
  schedule_expression = "cron(45 11 ? * MON-FRI *)"
  is_enabled = var.environment == "dev" ? true : false
}

resource "aws_cloudwatch_event_target" "suspension_service_ecs" {
  rule = aws_cloudwatch_event_rule.suspension_service_start_event.name
  arn  = aws_ecs_cluster.ecs-cluster.arn

  ecs_target {
    task_definition_arn = aws_ecs_task_definition.task.arn
  }
}