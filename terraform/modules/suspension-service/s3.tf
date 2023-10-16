locals {
  ingestion_bucket_name = "${var.environment}-ehr-ingestion"
}

resource "aws_s3_bucket" "ingestion_bucket" {
  bucket = local.ingestion_bucket_name

  tags = {
    Name        = local.ingestion_bucket_name
    Environment = var.environment
  }
}

resource "aws_s3_bucket_ownership_controls" "ingestion_bucket" {
  bucket = aws_s3_bucket.ingestion_bucket.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}


resource "aws_s3_bucket_acl" ingestion_bucket {
  depends_on = [aws_s3_bucket_ownership_controls.ingestion_bucket]
  bucket     = aws_s3_bucket.ingestion_bucket.id
  acl        = "private"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "ingestion_bucket" {
  bucket = aws_s3_bucket.ingestion_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "AES256"
    }
  }
}