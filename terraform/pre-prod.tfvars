environment    = "pre-prod"
log_level = "info"
grant_access_through_vpn = false
synthetic_patient_prefix = "96936"
service_desired_count = 1
scale_up_expression = "((HOUR(m1)>=14 && MINUTE(m1)>=45 )),10"
enable_scale_action = true