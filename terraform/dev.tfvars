environment    = "dev"
grant_access_through_vpn = true
synthetic_patient_prefix = "96937"
scale_up_expression = "( (MINUTE(m1)>=0 )),10, 0"
enable_scale_action = false