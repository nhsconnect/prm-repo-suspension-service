environment    = "pre-prod"
log_level = "info"
grant_access_through_vpn = false
synthetic_patient_prefix = "96936"

service_desired_count = 2
scale_up_expression = "((HOUR(m1)>=12 && MINUTE(m1)>=10 )),10"