# This file defines the outputs for the Terraform configuration.
# api_url is the endpoint for the API Gateway, and lambda_log_group is the log group for the Lambda function.
output "api_url" {
  value = aws_apigatewayv2_api.users_api.api_endpoint
}

# Lambda log group
# This output provides the log group for the Lambda function, which is where the logs for the
# Lambda function will be stored. The log group is named after the Lambda function.
output "lambda_log_group" {
  value = "/aws/lambda/${aws_lambda_function.java_lambda.function_name}"
}