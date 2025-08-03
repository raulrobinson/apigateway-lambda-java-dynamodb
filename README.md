## AWS Lambda with Java and Dynamodb

This project demonstrates how to create an AWS Lambda function using Java that interacts with DynamoDB. The Lambda function is triggered by an API Gateway event, processes the request, and performs operations on a DynamoDB table.

### Prerequisites

- AWS Account
- Java Development
- Gradle
- AWS CLI
- AWS SDK for Java
- DynamoDB Table
- AWS Lambda Function

### Setup Instructions

1. **Clone the Repository**: Clone this repository to your local machine.

    ```bash
    git clone
    ```
   
2. **Configure AWS CLI**: Ensure that you have the AWS CLI installed and configured with your credentials.

    ```bash
    aws configure
    ```
   
3. **Commands**: Use the following commands to build and deploy the Lambda function.

    ```bash
    cd lambda
    ./gradlew clean
    ./gradlew shadowJar
    cd ..
    terraform init
    terraform apply -auto-approve
    ```
   
4. **Destroy Resources**: To clean up the resources created by Terraform, run:

    ```bash
    terraform destroy -auto-approve
    ```
   

### Testing the Lambda Function

You can test the Lambda function using the AWS Management Console or by sending a test event through the AWS CLI. Make sure to provide the necessary input parameters as defined in your Lambda function.

1. **Create a User**: Send a POST request to the API Gateway endpoint with the user details in the request body.

    ```bash
    curl --location 'https://ov4lw30inl.execute-api.us-east-1.amazonaws.com/users' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "name": "Raul",
        "email": "raul@correo.com"
    }'
    ```

3. **Get a User**: Send a GET request to the API Gateway endpoint with the user ID as a path parameter.

    ```bash
    curl --location 'https://ov4lw30inl.execute-api.us-east-1.amazonaws.com/users/6e95c443-62d5-4685-b5c4-2826f63d07ad' \
    --header 'Content-Type: application/json'
    ```

3. **Update a User**: Send a PUT request to the API Gateway endpoint with the user ID as a path parameter and the updated user details in the request body.

    ```bash
    curl --location --request PUT 'https://ov4lw30inl.execute-api.us-east-1.amazonaws.com/users/6e95c443-62d5-4685-b5c4-2826f63d07ad' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "name": "Raul Actualizado",
        "email": "nuevo@correo.com"
    }'
    ```

4. **Delete a User**: Send a DELETE request to the API Gateway endpoint with the user ID as a path parameter.

    ```bash
    curl --location --request DELETE 'https://ov4lw30inl.execute-api.us-east-1.amazonaws.com/users/6e95c443-62d5-4685-b5c4-2826f63d07ad'
    ```

5. **List Users**: Send a GET request to the API Gateway endpoint to retrieve all users.

    ```bash
    curl --location 'https://ov4lw30inl.execute-api.us-east-1.amazonaws.com/users' \
    --header 'Content-Type: application/json'
    ```
   
### Author

- [Raul Bolivar](https://www.linkedin.com/in/rasysbox/)
- [GitHub](https://github.com/raulrobinson)

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
