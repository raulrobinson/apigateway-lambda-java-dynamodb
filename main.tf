# Region for AWS resources
provider "aws" {
  region = var.region
}

# Dynamodb table for storing user data
resource "aws_dynamodb_table" "users" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "Id"

  attribute {
    name = "Id"
    type = "S"
  }
}

# IAM role for Lambda function execution
# This role allows the Lambda function to write logs and access DynamoDB
resource "aws_iam_role" "lambda_exec" {
  name = "lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Attach the AWSLambdaBasicExecutionRole policy to the Lambda execution role
# This policy allows the Lambda function to write logs to CloudWatch
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Custom IAM policy for DynamoDB access
# This policy allows the Lambda function to perform CRUD operations on the DynamoDB table
# It includes permissions for PutItem, GetItem, UpdateItem, DeleteItem, and Scan
# The policy is attached to the Lambda execution role
# This is necessary for the Lambda function to interact with the DynamoDB table
resource "aws_iam_policy" "dynamodb_policy" {
  name = "lambda-dynamodb-policy"

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Action   = [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Scan"
      ],
      Effect   = "Allow",
      Resource = aws_dynamodb_table.users.arn
    }]
  })
}

# Attach the custom DynamoDB policy to the Lambda execution role
# This allows the Lambda function to access the DynamoDB table defined above
# The attachment is necessary for the Lambda function to perform operations on the DynamoDB table
# It ensures that the Lambda function has the necessary permissions to interact with the DynamoDB table
# This is crucial for the Lambda function to perform CRUD operations on user data stored in DynamoDB
# The policy is attached to the Lambda execution role created earlier
resource "aws_iam_role_policy_attachment" "lambda_dynamo_attach" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = aws_iam_policy.dynamodb_policy.arn
}

# Lambda function configuration
# This resource defines the AWS Lambda function that will handle HTTP requests
# The function is written in Java and uses the AWS Lambda Java runtime
# The function's handler is specified, which is the entry point for the Lambda function
# The function is associated with the IAM role created earlier, which allows it to execute with the necessary permissions
# The function's code is packaged as a JAR file, which is specified in the filename attribute
resource "aws_lambda_function" "java_lambda" {
  function_name = "users-handler"
  handler       = "com.example.Handler::handleRequest"
  runtime       = "java21"
  role          = aws_iam_role.lambda_exec.arn
  filename      = "${path.module}/lambda/build/libs/lambda.jar"
  memory_size   = 512
  timeout       = 20

  environment {
    variables = {
      TABLE_NAME = var.table_name
    }
  }
}

# API Gateway configuration
# This resource creates an HTTP API Gateway that will route requests to the Lambda function
# The API Gateway is configured to use the AWS_PROXY integration type, which allows it to directly invoke the Lambda function
# The integration URI is set to the Lambda function's invoke ARN, which is the endpoint that the API Gateway will call
# The payload format version is set to "1.0", which is the version of the payload format
resource "aws_apigatewayv2_api" "users_api" {
  name          = "users-http-api"
  protocol_type = "HTTP"
}

# Integration between API Gateway and Lambda function
# This resource defines the integration between the API Gateway and the Lambda function
# The integration type is set to "AWS_PROXY", which allows the API Gateway to directly invoke
# the Lambda function with the request payload
resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id                 = aws_apigatewayv2_api.users_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.java_lambda.invoke_arn
  payload_format_version = "1.0"
}

# API Gateway routes
# This resource defines the routes for the API Gateway
# Each route corresponds to an HTTP method and path for the user resource
# The routes are defined using a for_each loop to create multiple routes for different HTTP methods
resource "aws_apigatewayv2_route" "routes" {
  for_each = toset([
    "GET /users",
    "GET /users/{Id}",
    "POST /users",
    "PUT /users/{Id}",
    "DELETE /users/{Id}",
  ])
  api_id    = aws_apigatewayv2_api.users_api.id
  route_key = each.value
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

# API Gateway stage
# This resource creates a stage for the API Gateway
# The stage is named "$default", which is the default stage for the API Gateway
# The stage is configured to auto-deploy, meaning that changes to the API will be automatically
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.users_api.id
  name        = "$default"
  auto_deploy = true
}

# Lambda permission to allow API Gateway to invoke the Lambda function
# This resource grants the API Gateway permission to invoke the Lambda function
# The permission is specified using the aws_lambda_permission resource
resource "aws_lambda_permission" "allow_apigateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.java_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.users_api.execution_arn}/*/*"
}
