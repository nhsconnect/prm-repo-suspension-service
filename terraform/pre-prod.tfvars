environment    = "pre-prod"
log_level = "info"
synthetic_patient_prefix = "96936"
scale_up_expression = "((HOUR(m1)==12 && MINUTE(m1)==58 )),10, 0"
enable_scale_action = true