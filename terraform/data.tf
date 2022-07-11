data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "private_zone_id" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/private-root-zone-id"
}

data "aws_ssm_parameter" "deductions_private_private_subnets" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/deductions-private-private-subnets"
}

data "aws_ssm_parameter" "deductions_private_vpc_id" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/private-vpc-id"
}

data "aws_ssm_parameter" "deductions_private_db_subnets" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/deductions-private-database-subnets"
}

data "aws_ssm_parameter" "suspensions_kms_key_id" {
  name = "/repo/${var.environment}/output/prm-deductions-nems-event-processor/suspensions-kms-key-id"
}

data "aws_ssm_parameter" "suspensions_sns_topic_arn" {
  name = "/repo/${var.environment}/output/nems-event-processor/suspensions-sns-topic-arn"
}

data "aws_ssm_parameter" "pds_adaptor_auth_key" {
  name = "/repo/${var.environment}/user-input/api-keys/pds-adaptor/suspension-service"
}

data "aws_ssm_parameter" "repo_ods_code" {
  name = "/repo/${var.environment}/user-input/external/repository-ods-code"
}

data "aws_ssm_parameter" "safe_listed_patients_nhs_numbers" {
  name = "/repo/${var.environment}/user-input/external/safe-listed-patients-nhs-numbers"
}

data "aws_ssm_parameter" "splunk_audit_uploader_kms_key_id" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/splunk-audit-uploader-kms-key"
}

data "aws_ssm_parameter" "splunk_audit_uploader_queue_name" {
  name = "/repo/${var.environment}/output/prm-deductions-infra/splunk-audit-uploader-queue-name"
}