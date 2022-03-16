locals {
  suspensions_queue_name = "${var.environment}-${var.component_name}-suspensions-queue"
  not_suspended_queue_name = "${var.environment}-${var.component_name}-not-suspended-observability-queue"
  mof_updated_queue_name = "${var.environment}-${var.component_name}-mof-updated-queue"
  mof_not_updated_queue_name = "${var.environment}-${var.component_name}-mof-not-updated-queue"
  invalid_suspension_queue_name = "${var.environment}-${var.component_name}-invalid-suspension-dlq"
  non_sensitive_invalid_suspension_queue_name = "${var.environment}-${var.component_name}-invalid-suspension-dlq-audit"
  not_suspended_audit_queue_name = "${var.environment}-${var.component_name}-not-suspended-audit"
  event_out_of_order_audit_queue_name = "${var.environment}-${var.component_name}-out-of-order-audit"
  event_out_of_order_audit_splunk_dlq_queue_name = "${var.environment}-${var.component_name}-out-of-order-audit-splunk-dlq"
  mof_not_updated_audit_queue_name = "${var.environment}-${var.component_name}-mof-not-updated-audit"
  mof_updated_audit_queue_name = "${var.environment}-${var.component_name}-mof-updated-audit"
  deceased_patient_queue_name = "${var.environment}-${var.component_name}-deceased-patient-queue"
  deceased_patient_audit_queue_name = "${var.environment}-${var.component_name}-deceased-patient-audit"
  deceased_patient_audit_splunk_dlq_queue_name = "${var.environment}-${var.component_name}-deceased-patient-audit-splunk-dlq"
}

resource "aws_sqs_queue" "suspensions" {
  name                       = local.suspensions_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = data.aws_ssm_parameter.suspensions_kms_key_id.value
  receive_wait_time_seconds = 20
  visibility_timeout_seconds = 240

  tags = {
    Name = local.suspensions_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "suspensions_topic" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = data.aws_ssm_parameter.suspensions_sns_topic_arn.value
  endpoint             = aws_sqs_queue.suspensions.arn
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

resource "aws_sns_topic_subscription" "mof_not_updated" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.mof_not_updated.arn
  endpoint             = aws_sqs_queue.mof_not_updated.arn
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

resource "aws_sns_topic_subscription" "mof_updated" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.mof_updated.arn
  endpoint             = aws_sqs_queue.mof_updated.arn
}

resource "aws_sqs_queue" "invalid_suspension" {
  name                       = local.invalid_suspension_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.invalid_suspension.id

  tags = {
    Name = local.invalid_suspension_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "invalid_suspension" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.invalid_suspension.arn
  endpoint             = aws_sqs_queue.invalid_suspension.arn
}


resource "aws_sqs_queue" "non_sensitive_invalid_suspension" {
  name                       = local.non_sensitive_invalid_suspension_queue_name
  message_retention_seconds  = 1800
  kms_master_key_id = aws_kms_key.non_sensitive_invalid_suspension.id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.non_sensitive_invalid_suspension_splunk_dlq.arn
    maxReceiveCount     = 4
  })

  tags = {
    Name = local.non_sensitive_invalid_suspension_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "non_sensitive_invalid_suspension_splunk_dlq" {
  name                       = "${local.non_sensitive_invalid_suspension_queue_name}-splunk-dlq"
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.non_sensitive_invalid_suspension.id


  tags = {
    Name = "${local.non_sensitive_invalid_suspension_queue_name}-splunk-dlq"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "non_sensitive_invalid_suspension" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.non_sensitive_invalid_suspension.arn
  endpoint             = aws_sqs_queue.non_sensitive_invalid_suspension.arn
}

resource "aws_sqs_queue" "not_suspended_audit" {
  name                       = local.not_suspended_audit_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.not_suspended.id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.not_suspended_audit_dlq.arn
    maxReceiveCount     = 4
  })

  tags = {
    Name = local.not_suspended_audit_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "not_suspended_audit_dlq" {
  name                       = "${var.environment}-${var.component_name}-not-suspended-audit-dlq"
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.not_suspended.id

  tags = {
    Name = "${var.environment}-${var.component_name}-not-suspended-audit-dlq"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "not_suspended_audit" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.not_suspended.arn
  endpoint             = aws_sqs_queue.not_suspended_audit.arn
}

resource "aws_sqs_queue" "event_out_of_order_audit" {
  name                       = local.event_out_of_order_audit_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.event_out_of_order.id

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.event_out_of_order_audit_splunk_dlq.arn
    maxReceiveCount     = 4
  })

  tags = {
    Name = local.event_out_of_order_audit_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "event_out_of_order_audit_splunk_dlq" {
  name                       = local.event_out_of_order_audit_splunk_dlq_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.event_out_of_order.id

  tags = {
    Name = local.event_out_of_order_audit_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "event_out_of_order_observability_queue" {
  name                       = "${var.environment}-${var.component_name}-out-of-order-observability-queue"
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.event_out_of_order.id

  tags = {
    Name = "${var.environment}-${var.component_name}-out-of-order-observability-queue"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "event_out_of_order_audit" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.event_out_of_order.arn
  endpoint             = aws_sqs_queue.event_out_of_order_audit.arn
}

resource "aws_sns_topic_subscription" "event_out_of_order_observability_queue" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.event_out_of_order.arn
  endpoint             = aws_sqs_queue.event_out_of_order_observability_queue.arn
}

resource "aws_sqs_queue" "mof_not_updated_audit" {
  name                       = local.mof_not_updated_audit_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.mof_not_updated.id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.mof_not_updated_audit_dlq.arn
    maxReceiveCount     = 4
  })

  tags = {
    Name = local.mof_not_updated_audit_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "mof_not_updated_audit_dlq" {
  name                       = "${var.environment}-${var.component_name}-mof-not-updated-audit-dlq"
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.mof_not_updated.id

  tags = {
    Name = "${var.environment}-${var.component_name}-mof-not-updated-audit-dlq"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "mof_not_updated_audit" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.mof_not_updated.arn
  endpoint             = aws_sqs_queue.mof_not_updated_audit.arn
}

resource "aws_sqs_queue" "mof_updated_audit" {
  name                       = local.mof_updated_audit_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.mof_updated.id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.mof_updated_audit_dlq.arn
    maxReceiveCount     = 4
  })

  tags = {
    Name = local.mof_updated_audit_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "mof_updated_audit_dlq" {
  name                       = "${var.environment}-${var.component_name}-mof-updated-audit-dlq"
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.mof_updated.id

  tags = {
    Name = "${var.environment}-${var.component_name}-mof-updated-audit-dlq"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "mof_updated_audit" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.mof_updated.arn
  endpoint             = aws_sqs_queue.mof_updated_audit.arn
}

resource "aws_sqs_queue" "deceased_patient" {
  name                       = local.deceased_patient_queue_name
  message_retention_seconds  = 1800
  kms_master_key_id = aws_kms_key.deceased_patient.id
  receive_wait_time_seconds = 20
  visibility_timeout_seconds = 240

  tags = {
    Name = local.deceased_patient_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "deceased_patient" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.deceased_patient.arn
  endpoint             = aws_sqs_queue.deceased_patient.arn
}

resource "aws_sqs_queue" "deceased_patient_audit" {
  name                       = local.deceased_patient_audit_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.deceased_patient.id
  receive_wait_time_seconds = 20
  visibility_timeout_seconds = 240

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.deceased_patient_audit_splunk_dlq.arn
    maxReceiveCount     = 4
  })
  tags = {
    Name = local.deceased_patient_audit_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_sns_topic_subscription" "deceased_patient_audit" {
  protocol             = "sqs"
  raw_message_delivery = true
  topic_arn            = aws_sns_topic.deceased_patient.arn
  endpoint             = aws_sqs_queue.deceased_patient_audit.arn
}

resource "aws_sqs_queue" "deceased_patient_audit_splunk_dlq" {
  name                       = local.deceased_patient_audit_splunk_dlq_queue_name
  message_retention_seconds  = 1209600
  kms_master_key_id = aws_kms_key.deceased_patient.id
  receive_wait_time_seconds = 20
  visibility_timeout_seconds = 240

  tags = {
    Name = local.deceased_patient_audit_splunk_dlq_queue_name
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}
