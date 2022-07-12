output "repo_incoming_sns_topic" {
  value = aws_sns_topic.repo_incoming.arn
}

resource "aws_ssm_parameter" "repo_incoming_topic_arn" {
  name  = "/repo/${var.environment}/output/${var.component_name}/repo-incoming-topic-arn"
  type  = "String"
  value = aws_sns_topic.repo_incoming.arn
}

resource "aws_ssm_parameter" "repo_incoming_kms_key" {
  name  = "/repo/${var.environment}/output/${var.component_name}/repo-incoming-kms-key"
  type  = "String"
  value   = aws_kms_key.repo_incoming.id
}