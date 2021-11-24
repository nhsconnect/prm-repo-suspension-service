resource "aws_kms_key" "not_suspended" {
  description = "Custom KMS Key to enable server side encryption for SNS and SQS"
  policy      = data.aws_iam_policy_document.not_suspended_kms_key_policy_doc.json

  tags = {
    Name        = "${var.environment}-not-suspended-queue-encryption-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "not_suspended_encryption" {
  name          = "alias/not-suspended-encryption-kms-key"
  target_key_id = aws_kms_key.not_suspended.id
}

data "aws_iam_policy_document" "not_suspended_kms_key_policy_doc" {
  statement {
    effect = "Allow"

    principals {
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
      type        = "AWS"
    }
    actions   = ["kms:*"]
    resources = ["*"]
  }

  statement {
    effect = "Allow"

    principals {
      identifiers = ["sns.amazonaws.com"]
      type        = "Service"
    }

    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey*"
    ]

    resources = ["*"]
  }

  statement {
    effect = "Allow"

    principals {
      identifiers = ["cloudwatch.amazonaws.com"]
      type        = "Service"
    }

    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey*"
    ]

    resources = ["*"]
  }
}