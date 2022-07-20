component_name = "end-of-transfer-service"
repo_name = "end-of-transfer-service"
metric_namespace = "EndOfTransferService"
environment          = "perf"
environment_dns_zone = "perf.non-prod"

synthetic_patient_prefix        = "96936"
process_only_synthetic_or_safe_listed_patients = false

log_level = "info"

can_update_managing_organisation_to_repo = false
is_end_of_transfer_service = true
ecs_desired_count = 1