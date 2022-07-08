terraform{
      backend "s3" {
        bucket = "prm-deductions-terraform-state"
        key    = "end-of-transfer-service/terraform.tfstate"
        region = "eu-west-2"
        encrypt = true
    }
}
