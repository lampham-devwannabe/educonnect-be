#!/bin/bash
set -euo pipefail

FUNCTION_NAME="educonnect-video-transcoder"
REGION="ap-southeast-1"
ROLE_NAME="educonnect-lambda-execution-role"
S3_DEPLOY_BUCKET="educonnect-lambda-deployments"
ZIP_FILE="video-transcoder.zip"

cd "$(dirname "$0")"

zip -r9 "../${ZIP_FILE}" . -x "*.pyc" "__pycache__/*"
cd - >/dev/null

aws iam create-role --role-name "$ROLE_NAME" --assume-role-policy-document '{
  "Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}' || true
aws iam attach-role-policy --role-name "$ROLE_NAME" --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole || true
aws iam attach-role-policy --role-name "$ROLE_NAME" --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess || true
ROLE_ARN=$(aws iam get-role --role-name "$ROLE_NAME" --query 'Role.Arn' --output text)

aws s3 mb "s3://${S3_DEPLOY_BUCKET}" || true
aws s3 cp "${ZIP_FILE}" "s3://${S3_DEPLOY_BUCKET}/${ZIP_FILE}"

aws lambda create-function \
  --function-name "$FUNCTION_NAME" \
  --runtime python3.12 \
  --role "$ROLE_ARN" \
  --handler video-transcoder.lambda_handler \
  --code S3Bucket=${S3_DEPLOY_BUCKET},S3Key=${ZIP_FILE} \
  --timeout 900 --memory-size 3008 \
  --environment Variables="{S3_BUCKET_NAME=${S3_BUCKET_NAME},CALLBACK_API_URL=${CALLBACK_API_URL}}" \
  --region "$REGION" || \
aws lambda update-function-code --function-name "$FUNCTION_NAME" --s3-bucket "$S3_DEPLOY_BUCKET" --s3-key "$ZIP_FILE" --region "$REGION"

echo "Done. Configure S3 event trigger for prefix videos/lessons/ and suffix original.mp4."
