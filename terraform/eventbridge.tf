resource "aws_cloudwatch_event_rule" "suspension_service_start_event" {
  name        = "${var.environment}-${var.component_name}-start-event"
  description = "Eventbridge (formerly Cloudwatch event) rule to start Suspension Service"
  schedule_expression = "cron(45 11 ? * MON-FRI *)"
  is_enabled = var.environment == "dev" ? true : false
}

resource "aws_cloudwatch_event_target" "suspension_service_ecs" {
  rule = aws_cloudwatch_event_rule.suspension_service_start_event.name
  arn  = aws_ecs_cluster.ecs-cluster.arn
  role_arn  = aws_iam_role.suspension_service_start_event_role.arn

  ecs_target {
    task_definition_arn = aws_ecs_task_definition.task.arn
  }
}

resource "aws_iam_role" "suspension_service_start_event_role" {
  name = "${var.environment}-${var.component_name}-start-event-role"

  assume_role_policy = <<DOC
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "events.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
DOC
}

resource "aws_iam_role_policy" "suspension_service_start_event_role_policy" {
  name = "${var.environment}-${var.component_name}-start-event-role-policy"
  role = aws_iam_role.suspension_service_start_event_role.id

  policy = <<DOC
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "iam:PassRole",
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": "ecs:RunTask",
            "Resource": "${replace(aws_ecs_task_definition.task.arn, "/:\\d+$/", ":*")}"
        }
    ]
}
DOC
}