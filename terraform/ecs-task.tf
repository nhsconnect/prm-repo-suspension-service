locals {
  task_role_arn         = aws_iam_role.component-ecs-role.arn
  task_execution_role   = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${var.environment}-${var.component_name}-EcsTaskRole"
  task_ecr_url          = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com"
  task_log_group        = "/nhs/deductions/${var.environment}-${data.aws_caller_identity.current.account_id}/${var.component_name}"
  environment_variables = [
    { name = "NHS_ENVIRONMENT", value = var.environment },
    { name = "AWS_REGION", value = var.region },
    { name = "LOG_LEVEL", value = var.log_level },
    { name = "SUSPENSIONS_QUEUE_NAME", value = aws_sqs_queue.suspensions.name },
    { name = "NOT_SUSPENDED_SNS_TOPIC_ARN", value = aws_sns_topic.not_suspended.arn },
    { name = "NOT_SUSPENDED_QUEUE_NAME", value = aws_sqs_queue.not_suspended_observability.name },
    { name = "MOF_UPDATED_SNS_TOPIC_ARN", value = aws_sns_topic.mof_updated.arn },
    { name = "MOF_NOT_UPDATED_SNS_TOPIC_ARN", value = aws_sns_topic.mof_not_updated.arn },
    { name = "EVENT_OUT_OF_ORDER_SNS_TOPIC_ARN", value = aws_sns_topic.event_out_of_order.arn },
    { name = "INVALID_SUSPENSION_SNS_TOPIC_ARN", value = aws_sns_topic.invalid_suspension.arn },
    { name  = "NON_SENSITIVE_INVALID_SUSPENSION_SNS_TOPIC_ARN",
      value = aws_sns_topic.non_sensitive_invalid_suspension.arn
    },
    { name = "DECEASED_PATIENT_SNS_TOPIC_ARN", value = aws_sns_topic.deceased_patient.arn },
    { name = "PDS_ADAPTOR_SUSPENSION_SERVICE_PASSWORD", value = data.aws_ssm_parameter.pds_adaptor_auth_key.value },
    { name = "PROCESS_ONLY_SYNTHETIC_PATIENTS", value = tostring(var.process_only_synthetic_patients) },
    { name = "SYNTHETIC_PATIENT_PREFIX", value = var.synthetic_patient_prefix },
    { name = "DYNAMODB_TABLE_NAME", value = aws_dynamodb_table.suspensions.name },
    { name = "PDS_ADAPTOR_URL", value = "https://pds-adaptor.${var.environment}.non-prod.patient-deductions.nhs.uk" }
  ]
}

resource "aws_ecs_task_definition" "task" {
  family                   = var.component_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = local.task_execution_role
  task_role_arn            = local.task_role_arn


  container_definitions = templatefile("${path.module}/templates/ecs-task-def.tmpl", {
    container_name        = "${var.component_name}-container"
    ecr_url               = local.task_ecr_url,
    image_name            = "repo/${var.component_name}",
    image_tag             = var.task_image_tag,
    cpu                   = var.task_cpu,
    memory                = var.task_memory,
    log_region            = var.region,
    log_group             = local.task_log_group,
    environment_variables = jsonencode(local.environment_variables)
  })

  tags = {
    Environment = var.environment
    CreatedBy   = var.repo_name
  }
}

resource "aws_security_group" "ecs-tasks-sg" {
  name   = "${var.environment}-${var.component_name}-ecs-tasks-sg"
  vpc_id = data.aws_ssm_parameter.deductions_private_vpc_id.value

  egress {
    description = "Allow all outbound HTTPS traffic in vpc"
    protocol    = "tcp"
    from_port   = 443
    to_port     = 443
    cidr_blocks = [data.aws_vpc.private_vpc.cidr_block]
  }

  egress {
    description     = "Allow outbound HTTPS traffic to dynamodb"
    protocol        = "tcp"
    from_port       = 443
    to_port         = 443
    prefix_list_ids = [data.aws_ssm_parameter.dynamodb_prefix_list_id.value]
  }

  egress {
    description     = "Allow outbound HTTPS traffic to s3"
    protocol        = "tcp"
    from_port       = 443
    to_port         = 443
    prefix_list_ids = [data.aws_ssm_parameter.s3_prefix_list_id.value]
  }

  tags = {
    Name        = "${var.environment}-${var.component_name}-ecs-tasks-sg"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

data "aws_vpc" "private_vpc" {
  id = data.aws_ssm_parameter.deductions_private_vpc_id.value
}

data "aws_ssm_parameter" "dynamodb_prefix_list_id" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/dynamodb_prefix_list_id"
}

data "aws_ssm_parameter" "s3_prefix_list_id" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/s3_prefix_list_id"
}
