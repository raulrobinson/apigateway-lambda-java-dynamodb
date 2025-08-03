output "api_url" {
  value = aws_apigatewayv2_api.users_api.api_endpoint
}

output "lambda_log_group" {
  value = "/aws/lambda/${aws_lambda_function.java_lambda.function_name}"
}