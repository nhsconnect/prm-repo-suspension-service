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

resource "aws_ssm_parameter" "ecs_tasks_sg" {
  name  = "/repo/${var.environment}/output/${var.component_name}/ecs-sg-id"
  type  = "String"
  value = aws_security_group.ecs-tasks-sg.id
}

resource "aws_ssm_parameter" "ecs-cluster-name" {
  name  = "/repo/${var.environment}/output/${var.component_name}/suspension-service-ecs-cluster-name"
  type  = "String"
  value = aws_ecs_cluster.ecs-cluster.name
}

resource "aws_ssm_parameter" "repo_incoming_topic_arn" {
  name  = "/repo/${var.environment}/output/${var.component_name}/repo-incoming-topic-arn"
  type  = "String"
  value = var.is_end_of_transfer_service ? "" : aws_sns_topic.repo_incoming[0].arn
}

resource "aws_ssm_parameter" "repo_incoming_kms_key" {
  name  = "/repo/${var.environment}/output/${var.component_name}/repo-incoming-kms-key"
  type  = "String"
  value   = var.is_end_of_transfer_service ? "" : aws_kms_key.repo_incoming[0].id
}

resource "aws_ssm_parameter" "repo_incoming_observability_kms_key" {
  name  = "/repo/${var.environment}/output/${var.component_name}/repo-incoming-observability-kms-key"
  type  = "String"
  value = aws_kms_key.repo_incoming_observability.id
}

resource "aws_ssm_parameter" "repo_incoming_audit_topic_arn" {
  name  = "/repo/${var.environment}/output/${var.component_name}/repo-incoming-audit-topic-arn"
  type  = "String"
  value = aws_sns_topic.repo_incoming_audit.arn
}