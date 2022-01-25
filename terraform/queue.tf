locals {
  suspensions_queue_name = "${var.environment}-${var.component_name}-suspensions-queue"
  not_suspended_queue_name = "${var.environment}-${var.component_name}-not-suspended-observability-queue"
  mof_updated_queue_name = "${var.environment}-${var.component_name}-mof-updated-queue"
  mof_not_updated_queue_name = "${var.environment}-${var.component_name}-mof-not-updated-queue"

}

resource "aws_sqs_queue" "suspensions" {
  name                       = local.suspensions_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = data.aws_ssm_parameter.suspensions_kms_key_id.value
  receive_wait_time_seconds = 20

  tags = {
    Name = local.suspensions_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "suspensions_queue_name" {
  name  = "/repo/${var.environment}/output/${var.component_name}/suspensions-queue-name"
  type  = "String"
  value = aws_sqs_queue.suspensions.name
}

resource "aws_sns_topic_subscription" "suspensions_topic" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = data.aws_ssm_parameter.suspensions_sns_topic_arn.value
  endpoint             = aws_sqs_queue.suspensions.arn
}


resource "aws_ssm_parameter" "suspensions_queue_arn" {
  name  = "/repo/${var.environment}/output/${var.component_name}/suspensions-queue-arn"
  type  = "String"
  value = aws_sqs_queue.suspensions.arn
}

resource "aws_sns_topic" "not_suspended" {
  name = "${var.environment}-${var.component_name}-not-suspended-sns-topic"
  kms_master_key_id = aws_kms_key.not_suspended.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-not-suspended-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "not_suspended_observability" {
  name                       = local.not_suspended_queue_name
  message_retention_seconds  = 1800
  kms_master_key_id = aws_kms_key.not_suspended.id

  tags = {
    Name = local.not_suspended_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "not_suspended_observability_queue" {
  name  = "/repo/${var.environment}/output/${var.component_name}/not-suspended-observability-queue-name"
  type  = "String"
  value = aws_sqs_queue.not_suspended_observability.name
}


resource "aws_sns_topic_subscription" "not_suspended" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.not_suspended.arn
  endpoint             = aws_sqs_queue.not_suspended_observability.arn
}

resource "aws_sqs_queue_policy" "not_suspended_events_subscription" {
  queue_url = aws_sqs_queue.not_suspended_observability.id
  policy    = data.aws_iam_policy_document.not_suspended_events_policy_doc.json
}

data "aws_iam_policy_document" "not_suspended_events_policy_doc" {
  statement {

    effect = "Allow"

    actions = [
      "sqs:SendMessage"
    ]

    principals {
      identifiers = ["sns.amazonaws.com"]
      type        = "Service"
    }

    resources = [
      aws_sqs_queue.not_suspended_observability.arn
    ]

    condition {
      test     = "ArnEquals"
      values   = [aws_sns_topic.not_suspended.arn]
      variable = "aws:SourceArn"
    }
  }
}

resource "aws_sqs_queue_policy" "suspensions_subscription" {
  queue_url = aws_sqs_queue.suspensions.id
  policy    = data.aws_iam_policy_document.suspensions_sns_topic_access_to_queue.json
}

data "aws_iam_policy_document" "suspensions_sns_topic_access_to_queue" {
  statement {
    effect = "Allow"

    actions = [
      "sqs:SendMessage"
    ]

    principals {
      identifiers = ["sns.amazonaws.com"]
      type        = "Service"
    }

    resources = [
      aws_sqs_queue.suspensions.arn
    ]

    condition {
      test     = "ArnEquals"
      values   = [data.aws_ssm_parameter.suspensions_sns_topic_arn.value]
      variable = "aws:SourceArn"
    }
  }
}

resource "aws_sns_topic" "mof_updated" {
  name = "${var.environment}-${var.component_name}-mof-updated-sns-topic"
  kms_master_key_id = aws_kms_key.mof_updated.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-mof-updated-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic" "mof_not_updated" {
  name = "${var.environment}-${var.component_name}-mof-not-updated-sns-topic"
  kms_master_key_id = aws_kms_key.mof_not_updated.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-mof-updated-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "mof_not_updated" {
  name                       = local.mof_not_updated_queue_name
  message_retention_seconds  = 1800
  kms_master_key_id = aws_kms_key.mof_not_updated.id

  tags = {
    Name = local.mof_not_updated_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "mof_updated" {
  name                       = local.mof_updated_queue_name
  message_retention_seconds  = 1800
  kms_master_key_id = aws_kms_key.mof_updated.id

  tags = {
    Name = local.mof_updated_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "mof_updated_queue" {
  name  = "/repo/${var.environment}/output/${var.component_name}/mof-updated-queue-name"
  type  = "String"
  value = aws_sqs_queue.mof_updated.name
}

resource "aws_sns_topic_subscription" "mof_updated" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.mof_updated.arn
  endpoint             = aws_sqs_queue.mof_updated.arn
}

resource "aws_sns_topic_subscription" "mof_not_updated" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.mof_not_updated.arn
  endpoint             = aws_sqs_queue.mof_not_updated.arn
}

