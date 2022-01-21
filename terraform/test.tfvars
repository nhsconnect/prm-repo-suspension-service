environment    = "test"
grant_access_through_vpn = true
synthetic_patient_prefix = "96941"

service_desired_count = 1
scale_up_expression = "( (MINUTE(m1)==0 || MINUTE(m1)==15 || MINUTE(m1)==30 || MINUTE(m1)==45 )),10, 0"
