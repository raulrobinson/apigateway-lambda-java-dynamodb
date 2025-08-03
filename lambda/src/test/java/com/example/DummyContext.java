package com.example;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class DummyContext implements Context {
    @Override public String getAwsRequestId() { return "dummy-request-id"; }
    @Override public String getLogGroupName() { return "dummy-log-group"; }
    @Override public String getLogStreamName() { return "dummy-log-stream"; }
    @Override public String getFunctionName() { return "dummy-function"; }
    @Override public String getFunctionVersion() { return "1.0"; }
    @Override public String getInvokedFunctionArn() { return "dummy-arn"; }
    @Override public CognitoIdentity getIdentity() { return null; }
    @Override public ClientContext getClientContext() { return null; }
    @Override public int getRemainingTimeInMillis() { return 1000; }
    @Override public int getMemoryLimitInMB() { return 512; }
    @Override public LambdaLogger getLogger() {
        return new LambdaLogger() {
            @Override
            public void log(String message) {
                System.out.println("LOG: " + message);
            }

            @Override
            public void log(byte[] message) {
                System.out.println("LOG: " + new String(message));
            }
        };
    }
}

