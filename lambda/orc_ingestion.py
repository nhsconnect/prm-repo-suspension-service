import logging
import boto3
import json
import uuid
import os
import re

from datetime import datetime, timezone


logger = logging.getLogger()
logger.setLevel(logging.INFO)

sqs_client = boto3.client("sqs")
s3_client = boto3.client("s3")


def lambda_handler(event, context):
    """
    When using this lambda, please include previous_ods_code in the event json.
    Example:
    {"previous_ods_code": "ods_code"}
    """
    try:
        previous_ods_code = event["previous_ods_code"]
    except KeyError as e:
        return {"statusCode": 400, "error": "missing param 'previous_ods_code'"}

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
    response = s3_client.get_object(
        Bucket=bucket_name,
        Key=filename
    )

    file_lines = response["Body"].readlines()

    if len(file_lines) > 1:
        raise InvalidFileFormatException(
            'All NHS numbers must be contained on a single line, seperated by commas.'
        )

    nhs_numbers_str = file_lines[0].decode('utf-8')

    only_nums_and_commas_bool = bool(re.match('^[0-9,]+$', nhs_numbers_str))

    if not only_nums_and_commas_bool:
        raise InvalidFileFormatException(
            'File should only contain numbers and commas on a single line.'
        )

    nhs_number_list = nhs_numbers_str.split(',')
    all_length_10_bool = all(len(x) == 10 for x in nhs_number_list)

    if not all_length_10_bool:
        raise InvalidNhsNumberException(
            'All NHS numbers must be 10 digits long and there must be no trailing commas.'
        )

    return nhs_number_list


def new_uuid() -> str:
    return str(uuid.uuid4())


def get_timestamp() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def build_suspension_message(nhs_number: str, previous_ods_code: str, nems_message_id: str) -> str:
    return json.dumps({
        "nhsNumber": nhs_number,
        "lastUpdated": get_timestamp(),
        "previousOdsCode": previous_ods_code,
        "nemsMessageId": nems_message_id,
    })


def send_message_with_trace_id(message_body: str, queue_url: str, trace_id: str):
    return sqs_client.send_message(
        QueueUrl=queue_url,
        MessageAttributes={
            "traceId": {
                "DataType": "String",
                "StringValue": trace_id
            }
        },
        MessageBody=message_body,
    )


class InvalidFileFormatException(Exception):
    pass


class InvalidNhsNumberException(Exception):
    pass
