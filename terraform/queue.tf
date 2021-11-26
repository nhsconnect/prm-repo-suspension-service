locals {
  suspensions_queue_name = "${var.environment}-${var.component_name}-suspensions-queue"
  not_suspended_queue_name = "${var.environment}-${var.component_name}-not-suspended-observability-queue"
}

resource "aws_sqs_queue" "suspensions" {
  name                       = local.suspensions_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = data.aws_ssm_parameter.suspensions_kms_key_id.value

  tags = {
    Name = local.suspensions_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic" "not_suspended" {
  name = "${var.environment}-${var.component_name}-not-suspended-sns-topic"
  kms_master_key_id = aws_kms_key.not_suspended.id
  sns_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-not-suspended-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "not_suspended" {
  name                       = local.not_suspended_queue_name
  message_retention_seconds  = 1800
  kms_master_key_id = aws_kms_key.not_suspended.id

  tags = {
    Name = local.not_suspended_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "not_suspended" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.not_suspended.arn
  endpoint             = aws_sqs_queue.not_suspended.arn
}

resource "aws_sqs_queue_policy" "not_suspended_events_subscription" {
  queue_url = aws_sqs_queue.not_suspended.id
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
      aws_sqs_queue.not_suspended.arn
    ]

    condition {
      test     = "ArnEquals"
      values   = [aws_sns_topic.not_suspended.arn]
      variable = "aws:SourceArn"
    }
  }
}
