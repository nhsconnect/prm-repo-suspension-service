resource "aws_kms_key" "not_suspended" {
  description = "Custom KMS Key to enable server side encryption for SNS and SQS"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json
  enable_key_rotation = true

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
  enable_key_rotation = true

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
  enable_key_rotation = true

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
  enable_key_rotation = true

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
  enable_key_rotation = true

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

resource "aws_kms_key" "event_out_of_order" {
  description = "Custom KMS Key to enable server side encryption for event out of order topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json
  enable_key_rotation = true

  tags = {
    Name        = "${var.environment}-event-out-of-order-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "event_out_of_order_encryption" {
  name          = "alias/event-out-of-order-encryption-kms-key"
  target_key_id = aws_kms_key.event_out_of_order.id
}

resource "aws_kms_key" "suspension_dynamodb_kms_key" {
  description = "Custom KMS Key to enable server side encryption for Suspension DB"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json
  enable_key_rotation = true

  tags = {
    Name        = "${var.environment}-${var.component_name}-dynamodb-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_key" "deceased_patient" {
  description = "Custom KMS Key to enable server side encryption for deceased patient topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json
  enable_key_rotation = true

  tags = {
    Name        = "${var.environment}-deceased-patient-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "deceased_patient_encryption" {
  name          = "alias/deceased-patient-encryption-kms-key"
  target_key_id = aws_kms_key.deceased_patient.id
}

resource "aws_kms_key" "repo_incoming" {
  description = "Custom KMS Key to enable server side encryption for repo incoming topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json
  enable_key_rotation = true

  tags = {
    Name        = "${var.environment}-repo-incoming-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "repo_incoming_encryption" {
  name          = "alias/repo-incoming-encryption-kms-key"
  target_key_id = aws_kms_key.repo_incoming.id
}

resource "aws_kms_key" "repo_incoming_observability" {
  description = "Custom KMS Key to enable server side encryption for repo incoming observability topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json
  enable_key_rotation = true

  tags = {
    Name        = "${var.environment}-repo-incoming-observability-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "repo_incoming_observability_encryption" {
  name          = "alias/repo-incoming-observability-encryption-kms-key"
  target_key_id = aws_kms_key.repo_incoming_observability.id
}

resource "aws_kms_key" "repo_incoming_audit" {
  description = "Custom KMS Key to enable server side encryption for repo incoming audit topic"
  policy      = data.aws_iam_policy_document.kms_key_policy_doc.json
  enable_key_rotation = true

  tags = {
    Name        = "${var.environment}-repo-incoming-audit-kms-key"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_kms_alias" "repo_incoming_audit_encryption" {
  name          = "alias/repo-incoming-audit-encryption-kms-key"
  target_key_id = aws_kms_key.repo_incoming_audit.id
}