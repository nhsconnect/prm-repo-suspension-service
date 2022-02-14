locals {
  account_id = data.aws_caller_identity.current.account_id
}

data "aws_iam_policy_document" "ecs-assume-role-policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type = "Service"
      identifiers = [
        "ecs-tasks.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "component-ecs-role" {
  name               = "${var.environment}-${var.component_name}-EcsTaskRole"
  assume_role_policy = data.aws_iam_policy_document.ecs-assume-role-policy.json
  description        = "Role assumed by ${var.component_name} ECS task"

  tags = {
    Environment = var.environment
    CreatedBy   = var.repo_name
  }
}

data "aws_iam_policy_document" "ecr_policy_doc" {
  statement {
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage"
    ]

    resources = [
      "arn:aws:ecr:${var.region}:${local.account_id}:repository/repo/${var.component_name}"
    ]
  }

  statement {
    actions = [
      "ecr:GetAuthorizationToken"
    ]

    resources = [
      "*"
    ]
  }
}

data "aws_iam_policy_document" "logs_policy_doc" {
  statement {
    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]

    resources = [
      "arn:aws:logs:${var.region}:${local.account_id}:log-group:/nhs/deductions/${var.environment}-${local.account_id}/${var.component_name}:*"
    ]
  }
}

data "aws_iam_policy_document" "cloudwatch_metrics_policy_doc" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
      "cloudwatch:GetMetricData"
    ]

    resources = ["*"]
    condition {
      test     = "StringEquals"
      values   = [aws_cloudwatch_metric_alarm.health_metric_failure_alarm.namespace]
      variable = "cloudwatch:namespace"
    }
  }
}

resource "aws_iam_role_policy_attachment" "suspensions_processor_sqs" {
  role       = aws_iam_role.component-ecs-role.name
  policy_arn = aws_iam_policy.suspensions_processor_sqs.arn
}

resource "aws_iam_policy" "suspensions_processor_sqs" {
  name   = "${var.environment}-${var.component_name}-sqs"
  policy = data.aws_iam_policy_document.sqs_suspensions_ecs_task.json
}

data "aws_iam_policy_document" "sqs_suspensions_ecs_task" {
  statement {
    actions = [
      "sqs:GetQueue*",
      "sqs:ChangeMessageVisibility",
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage"
    ]
    resources = [
      aws_sqs_queue.suspensions.arn,
      aws_sqs_queue.not_suspended_observability.arn,
      aws_sqs_queue.mof_updated.arn,
      aws_sqs_queue.mof_not_updated.arn,
      aws_sqs_queue.invalid_suspension.arn,
      aws_sqs_queue.non_sensitive_invalid_suspension.arn,
      aws_sqs_queue.event_out_of_date_audit.arn,
      aws_sqs_queue.not_suspended_audit.arn,
      aws_sqs_queue.mof_not_updated_audit.arn,
      aws_sqs_queue.mof_updated_audit.arn
    ]
  }
}
resource "aws_iam_policy" "not_suspended_processor_sns" {
  name   = "${var.environment}-${var.component_name}-sns"
  policy = data.aws_iam_policy_document.sns_policy_doc.json
}

resource "aws_iam_role_policy_attachment" "nems_events_processor_sns" {
  role       = aws_iam_role.component-ecs-role.name
  policy_arn = aws_iam_policy.not_suspended_processor_sns.arn
}

data "aws_iam_policy_document" "sns_policy_doc" {
  statement {
    actions = [
      "sns:Publish",
      "sns:GetTopicAttributes"
    ]
    resources = [
      aws_sns_topic.not_suspended.arn,
      aws_sns_topic.mof_updated.arn,
      aws_sns_topic.mof_not_updated.arn,
      aws_sns_topic.invalid_suspension.arn,
      aws_sns_topic.non_sensitive_invalid_suspension.arn
    ]
  }
}

resource "aws_iam_role_policy_attachment" "suspensions_kms" {
  role       = aws_iam_role.component-ecs-role.name
  policy_arn = aws_iam_policy.suspensions_kms.arn
}

resource "aws_iam_policy" "suspensions_kms" {
  name   = "${var.environment}-${var.component_name}-kms"
  policy = data.aws_iam_policy_document.kms_policy_doc.json
}

data "aws_iam_policy_document" "kms_policy_doc" {
  statement {
    actions = [
      "kms:*"
    ]
    resources = [
      "*"
    ]
  }
}
resource "aws_iam_policy" "ecr_policy" {
  name   = "${var.environment}-${var.component_name}-ecr"
  policy = data.aws_iam_policy_document.ecr_policy_doc.json
}

resource "aws_iam_policy" "logs_policy" {
  name   = "${var.environment}-${var.component_name}-logs"
  policy = data.aws_iam_policy_document.logs_policy_doc.json
}

resource "aws_iam_policy" "cloudwatch_metrics_policy" {
  name   = "${var.environment}-${var.component_name}-cloudwatch-metrics"
  policy = data.aws_iam_policy_document.cloudwatch_metrics_policy_doc.json
}

resource "aws_iam_role_policy_attachment" "ecr_policy_attach" {
  role       = aws_iam_role.component-ecs-role.name
  policy_arn = aws_iam_policy.ecr_policy.arn
}

resource "aws_iam_role_policy_attachment" "logs_policy_attach" {
  role       = aws_iam_role.component-ecs-role.name
  policy_arn = aws_iam_policy.logs_policy.arn
}

resource "aws_iam_role_policy_attachment" "cloudwatch_metrics_policy_attach" {
  role       = aws_iam_role.component-ecs-role.name
  policy_arn = aws_iam_policy.cloudwatch_metrics_policy.arn
}

resource "aws_iam_role" "sns_failure_feedback_role" {
  name               = "${var.environment}-${var.component_name}-sns-failure-feedback-role"
  assume_role_policy = data.aws_iam_policy_document.sns_service_assume_role_policy.json
  description        = "Allows logging of SNS delivery failures in ${var.component_name}"

  tags = {
    Environment = var.environment
    CreatedBy   = var.repo_name
  }
}

data "aws_iam_policy_document" "sns_failure_feedback_policy" {
  statement {
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:PutMetricFilter",
      "logs:PutRetentionPolicy"
    ]
    resources = [
      "*"
    ]
  }
}

resource "aws_iam_policy" "sns_failure_feedback_policy" {
  name   = "${var.environment}-${var.component_name}-sns-failure-feedback"
  policy = data.aws_iam_policy_document.sns_failure_feedback_policy.json
}

resource "aws_iam_role_policy_attachment" "sns_failure_feedback_policy_attachment" {
  role       = aws_iam_role.sns_failure_feedback_role.name
  policy_arn = aws_iam_policy.sns_failure_feedback_policy.arn
}

data "aws_iam_policy_document" "sns_service_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type = "Service"
      identifiers = [
        "sns.amazonaws.com"
      ]
    }
  }
}

resource "aws_sqs_queue_policy" "mof_updated_events_subscription" {
  queue_url = aws_sqs_queue.mof_updated.id
  policy    = data.aws_iam_policy_document.mof_updated_events_policy_doc.json
}

resource "aws_sqs_queue_policy" "mof_not_updated_events_subscription" {
  queue_url = aws_sqs_queue.mof_not_updated.id
  policy    = data.aws_iam_policy_document.mof_not_updated_events_policy_doc.json
}

resource "aws_sqs_queue_policy" "not_suspended_events_subscription" {
  queue_url = aws_sqs_queue.not_suspended_observability.id
  policy    = data.aws_iam_policy_document.not_suspended_events_policy_doc.json
}

resource "aws_sqs_queue_policy" "suspensions_subscription" {
  queue_url = aws_sqs_queue.suspensions.id
  policy    = data.aws_iam_policy_document.suspensions_sns_topic_access_to_queue.json
}

resource "aws_sqs_queue_policy" "invalid_suspension_subscription" {
  queue_url = aws_sqs_queue.invalid_suspension.id
  policy    = data.aws_iam_policy_document.invalid_suspension_policy_doc.json
}

resource "aws_sqs_queue_policy" "non_sensitive_invalid_suspension_subscription" {
  queue_url = aws_sqs_queue.non_sensitive_invalid_suspension.id
  policy    = data.aws_iam_policy_document.non_sensitive_invalid_suspension_policy_doc.json
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

data "aws_iam_policy_document" "mof_updated_events_policy_doc" {
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
      aws_sqs_queue.mof_updated.arn
    ]

    condition {
      test     = "ArnEquals"
      values   = [aws_sns_topic.mof_updated.arn]
      variable = "aws:SourceArn"
    }
  }
}

data "aws_iam_policy_document" "mof_not_updated_events_policy_doc" {
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
      aws_sqs_queue.mof_not_updated.arn
    ]

    condition {
      test     = "ArnEquals"
      values   = [aws_sns_topic.mof_not_updated.arn]
      variable = "aws:SourceArn"
    }
  }
}

data "aws_iam_policy_document" "invalid_suspension_policy_doc" {
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
      aws_sqs_queue.invalid_suspension.arn
    ]

    condition {
      test     = "ArnEquals"
      values   = [aws_sns_topic.invalid_suspension.arn]
      variable = "aws:SourceArn"
    }
  }
}

data "aws_iam_policy_document" "non_sensitive_invalid_suspension_policy_doc" {
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
      aws_sqs_queue.non_sensitive_invalid_suspension.arn
    ]

    condition {
      test     = "ArnEquals"
      values   = [aws_sns_topic.non_sensitive_invalid_suspension.arn]
      variable = "aws:SourceArn"
    }
  }
}