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

resource "aws_sns_topic" "invalid_suspension" {
  name = "${var.environment}-${var.component_name}-invalid-suspension-sns-topic"
  kms_master_key_id = aws_kms_key.invalid_suspension.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-invalid-suspension-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic" "non_sensitive_invalid_suspension" {
  name = "${var.environment}-${var.component_name}-non-sensitive-invalid-suspension-sns-topic"
  kms_master_key_id = aws_kms_key.non_sensitive_invalid_suspension.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-non-sensitive-invalid-suspension-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic" "event_out_of_order" {
  name = "${var.environment}-${var.component_name}-event-out-of-order"
  kms_master_key_id = aws_kms_key.event_out_of_order.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-event-out-of-order-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic" "deceased_patient" {
  name = "${var.environment}-${var.component_name}-deceased-patient"
  kms_master_key_id = aws_kms_key.deceased_patient.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-deceased-patient-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic" "repo_incoming" {
  name = "${var.environment}-${var.component_name}-repo-incoming"
  kms_master_key_id = aws_kms_key.repo_incoming.id
  sqs_failure_feedback_role_arn = aws_iam_role.sns_failure_feedback_role.arn

  tags = {
    Name = "${var.environment}-${var.component_name}-repo-incoming-sns-topic"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}


data "aws_sns_topic" "alarm_notifications" {
  name = "${var.environment}-alarm-notifications-sns-topic"
}

