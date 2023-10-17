import json
import logging
import os
import uuid
from datetime import datetime, timezone

import boto3

logger = logging.getLogger()
logger.setLevel(logging.INFO)

sqs_client = boto3.client("sqs")
s3_client = boto3.client("s3")


def lambda_handler(event, context):
    previous_ods_code = "M85019"
    file_to_ingest = os.environ["INGEST_FILE_NAME"]  # this env var is set as "Patient-List-Test" in terraform

    ingestion_bucket_name = os.environ["S3_BUCKET_NAME"]
    suspension_queue_url = os.environ["SUSPENSION_QUEUE_URL"]

    nhs_number_list: list[str] = get_nhs_number_list_from_s3(filename=file_to_ingest, bucket_name=ingestion_bucket_name)
    all_trace_ids = []

    for nhs_number in nhs_number_list:
        trace_id = new_uuid()
        all_trace_ids.append(trace_id)
        logger.info(f"Assigned a new traceId: {trace_id}")

        suspension_message_json = build_suspension_message(
            nhs_number=nhs_number,
            previous_ods_code=previous_ods_code,
            nems_message_id=trace_id,
        )
        send_message_with_trace_id(
            message_body=suspension_message_json,
            queue_url=suspension_queue_url,
            trace_id=trace_id,
        )
        logger.info(
            f"sent message for a patient to queue with traceId: {trace_id}"
        )

    logger.info("Here are all the trace ids that related to this ingest:")
    logger.info(str(all_trace_ids))

    return {"statusCode": 200, "body": str(all_trace_ids)}


def get_nhs_number_list_from_s3(filename: str, bucket_name: str) -> list:
    local_file_path = f"/tmp/{filename}"
    s3_client.download_file(
        bucket_name, filename, local_file_path
    )
    with open(local_file_path, "r") as f:
        nhs_number_list = f.read()
    os.remove(local_file_path)
    return nhs_number_list.split(',')


def new_uuid() -> str:
    return str(uuid.uuid4())


def get_timestamp():
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def build_suspension_message(
        nhs_number: str, previous_ods_code: str, nems_message_id: str
) -> str:
    return json.dumps({
        "nhsNumber": nhs_number,
        "lastUpdated": get_timestamp(),
        "previousOdsCode": previous_ods_code,
        "nemsMessageId": nems_message_id,
    })


def send_message_with_trace_id(message_body: str, queue_url: str, trace_id: str):
    return sqs_client.send_message(
        QueueUrl=queue_url,
        MessageAttributes={"traceId": {"DataType": "String", "StringValue": trace_id}},
        MessageBody=message_body,
    )
