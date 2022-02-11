resource "aws_ssm_parameter" "suspensions_queue_name" {
  name  = "/repo/${var.environment}/output/${var.component_name}/suspensions-queue-name"
  type  = "String"
  value = aws_sqs_queue.suspensions.name
}

resource "aws_ssm_parameter" "suspensions_queue_arn" {
  name  = "/repo/${var.environment}/output/${var.component_name}/suspensions-queue-arn"
  type  = "String"
  value = aws_sqs_queue.suspensions.arn
}

resource "aws_ssm_parameter" "mof_updated_queue" {
  name  = "/repo/${var.environment}/output/${var.component_name}/mof-updated-queue-name"
  type  = "String"
  value = aws_sqs_queue.mof_updated.name
}

resource "aws_ssm_parameter" "ecs_tasks_sq" {
  name  = "/repo/${var.environment}/output/${var.component_name}/ecs-sg-id"
  type  = "String"
  value = aws_security_group.ecs-tasks-sg.id
}

resource "aws_ssm_parameter" "ecs-cluster-name" {
  name  = "/repo/${var.environment}/output/${var.component_name}/suspension-service-ecs-cluster-name"
  type  = "String"
  value = aws_ecs_cluster.ecs-cluster.name
}