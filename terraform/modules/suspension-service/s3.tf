locals {
  ingestion_bucket_name = "${var.environment}-ehr-ingestion"
}

resource "aws_s3_bucket" "ingestion_bucket" {
  bucket = local.ingestion_bucket_name
  acl           = "private"
  tags = {
    Name        = local.ingestion_bucket_name
    Environment = var.environment
  }
}
