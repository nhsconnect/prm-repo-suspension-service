environment          = "test"
environment_dns_zone = "test.non-prod"

synthetic_patient_prefix = "96941"
process_only_synthetic_patients = true

scale_up_expression = "( (MINUTE(m1)==0 || MINUTE(m1)==15 || MINUTE(m1)==30 || MINUTE(m1)==45 )),10, 0"

can_update_managing_organisation_to_repo = false