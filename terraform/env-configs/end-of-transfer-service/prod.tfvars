component_name = "end-of-transfer-service"
repo_name = "end-of-transfer-service"
metric_namespace = "EndOfTransferService"
environment              = "prod"
environment_dns_zone     = "prod"

synthetic_patient_prefix = "999"
process_only_synthetic_patients = true

can_update_managing_organisation_to_repo = false
is_end_of_transfer_service = true
ecs_desired_count = 1