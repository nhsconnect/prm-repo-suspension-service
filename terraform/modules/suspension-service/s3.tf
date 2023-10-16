locals {
  ingestion_bucket_name = "${var.environment}-ehr-ingestion"
}

resource "aws_s3_bucket" "ingestion_bucket" {
  bucket = local.ingestion_bucket_name
  acl    = "private"
  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
    tags = {
      Name        = local.ingestion_bucket_name
      Environment = var.environment
    }
  }
}

  resource "aws_s3_bucket_policy" "ingestion_bucket_policy" {
    bucket = aws_s3_bucket.ingestion_bucket.id
    policy = jsonencode({
      "Statement" : [
        {
          Effect : "Deny",
          Principal : "*",
          Action : "s3:*",
          Resource : "${aws_s3_bucket.ingestion_bucket.arn}/*",
          Condition : {
            Bool : {
              "aws:SecureTransport" : "false"
            }
          }
        }
      ]
    }
    )
  }


