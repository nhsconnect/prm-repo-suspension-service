resource "aws_kms_key" "not_suspended" {
  description = "Custom KMS Key to enable server side encryption for SNS and SQS"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json

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

data "aws_iam_policy_document" "kms_key_policy_doc" {
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

resource "aws_kms_key" "mof_updated" {
  description = "Custom KMS Key to enable server side encryption for SNS and SQS"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json

  tags = {
    Name        = "${var.environment}-mof-updated-queue-encryption-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "mof_updated_encryption" {
  name          = "alias/mof-updated-encryption-kms-key"
  target_key_id = aws_kms_key.mof_updated.id
}

resource "aws_kms_key" "mof_not_updated" {
  description = "Custom KMS Key to enable server side encryption for mof not updated topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json

  tags = {
    Name        = "${var.environment}-mof-not-updated-queue-encryption-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "mof_not_updated_encryption" {
  name          = "alias/mof-not-updated-encryption-kms-key"
  target_key_id = aws_kms_key.mof_not_updated.id
}

resource "aws_kms_key" "invalid_suspension" {
  description = "Custom KMS Key to enable server side encryption for invalid suspension topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json

  tags = {
    Name        = "${var.environment}-invalid-suspension-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "invalid_suspension_encryption" {
  name          = "alias/invalid-suspension-encryption-kms-key"
  target_key_id = aws_kms_key.invalid_suspension.id
}

resource "aws_kms_key" "non_sensitive_invalid_suspension" {
  description = "Custom KMS Key to enable server side encryption for non sensitive invalid suspension topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json

  tags = {
    Name        = "${var.environment}-non-sensitive-invalid-suspension-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "non_sensitive_invalid_suspension_encryption" {
  name          = "alias/non-sensitive-invalid-suspension-encryption-kms-key"
  target_key_id = aws_kms_key.non_sensitive_invalid_suspension.id
}
