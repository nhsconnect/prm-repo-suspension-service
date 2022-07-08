module "shared_cloudwatch" {
  source = "../shared"
  environment = var.environment
  component_name = var.component_name
  repo_name = var.repo_name
  account_id = data.aws_caller_identity.current.account_id
  alarm_notifications_arn = data.aws_sns_topic.alarm_notifications.arn
  event_out_of_order_audit_queue_name = aws_sqs_queue.event_out_of_order_audit.name
  not_suspended_audit_queue_name = aws_sqs_queue.not_suspended_audit.name
  mof_not_updated_audit_queue_name = aws_sqs_queue.mof_not_updated_audit.name
  mof_updated_audit_queue_name = aws_sqs_queue.mof_updated_audit.name
  deceased_patient_audit_queue_name = aws_sqs_queue.deceased_patient_audit.name
  invalid_suspension_audit_queue_name = aws_sqs_queue.invalid_suspension_audit.name
}
# moved block requires higher version of terraform
# alternatively use "terraform state mv" but it's a worse option
# https://www.terraform.io/cli/commands/state/mv

#moved {
#  from = aws_cloudwatch_log_group.log_group
#  to = module.shared_cloudwatch.aws_cloudwatch_log_group.log_group
#}
#
#moved {
#  from = aws_cloudwatch_log_metric_filter.log_metric_filter
#  to = module.shared_cloudwatch.aws_cloudwatch_log_metric_filter.log_metric_filter
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.error_log_alarm
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.error_log_alarm
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.not_suspended_sns_topic_error_log_alarm
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.not_suspended_sns_topic_error_log_alarm
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_out_of_order_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_out_of_order_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_not_suspended_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_not_suspended_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_mof_not_updated_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_mof_not_updated_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_mof_updated_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_mof_updated_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_deceased_patient_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_deceased_patient_audit
#}
#
#moved {
#  from = aws_cloudwatch_metric_alarm.suspension_invalid_suspension_dlq_audit
#  to = module.shared_cloudwatch.aws_cloudwatch_metric_alarm.suspension_invalid_suspension_dlq_audit
#}