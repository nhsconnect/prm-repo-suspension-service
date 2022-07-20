component_name = "end-of-transfer-service"
repo_name = "end-of-transfer-service"
metric_namespace = "EndOfTransferService"
environment          = "dev"
environment_dns_zone = "dev.non-prod"

synthetic_patient_prefix = "96937"

scale_up_expression = "( (MINUTE(m1)>=0 )),10, 0"
enable_scale_action = false
can_update_managing_organisation_to_repo = false
is_end_of_transfer_service = true
ecs_desired_count = 1
