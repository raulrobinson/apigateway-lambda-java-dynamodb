provider "aws" {
  region = var.region
}

resource "aws_dynamodb_table" "users" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "Id"

  attribute {
    name = "Id"
    type = "S"
  }
}

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

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

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

resource "aws_iam_role_policy_attachment" "lambda_dynamo_attach" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = aws_iam_policy.dynamodb_policy.arn
}

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

resource "aws_apigatewayv2_api" "users_api" {
  name          = "users-http-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id                 = aws_apigatewayv2_api.users_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.java_lambda.invoke_arn
  payload_format_version = "1.0"
}

# Rutas RESTful con path parameter opcional
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

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.users_api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "allow_apigateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.java_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.users_api.execution_arn}/*/*"
}
