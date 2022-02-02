environment    = "test"
synthetic_patient_prefix = "96941"
scale_up_expression = "( (MINUTE(m1)==0 || MINUTE(m1)==15 || MINUTE(m1)==30 || MINUTE(m1)==45 )),10, 0"
enable_scale_action = true