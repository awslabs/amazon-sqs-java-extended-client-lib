/*
 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.sqs.javamessaging;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Tests the AmazonSQSExtendedClient class.
 */
public class AmazonSQSExtendedClientIntegrationTest {

    private AmazonSQSClient client;
    private AmazonSQS sqs;
    private AmazonS3 s3;
    private static final AWSCredentials AWS_CREDS = new BasicAWSCredentials("[YOUR_AWS_KEY]", "[YOUR_AWS_SECRET]");
    private static final String S3_BUCKET_NAME = "[YOUR_EXISTING_S3_BUCKET_NAME]";
    private static final String SQS_QUEUE_URL = "[YOUR_EXISTING_SQS_QUEUE_NAME]";
    private static final int SQS_SIZE_LIMIT = 262144;

    /**
     * A simple S3KeyGenerator implementation that pre-pends the current date in front of the unique S3 key name.
     */
    class DateKeyGenerator implements S3KeyGenerator {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        @Override
        public String generateObjectKey(SendMessageRequest sendMessageRequest) {
            return formatter.format(Calendar.getInstance().getTime()) + "/" + UUID.randomUUID().toString();
        }

        @Override
        public String generateObjectKey(SendMessageBatchRequestEntry batchEntry) {
            return formatter.format(Calendar.getInstance().getTime()) + "/" + UUID.randomUUID().toString();
        }
    }

    /**
     * A S3KeyGenerator implementation that uses the message meta data to construct a unique S3 key name.
     */
    class MetaBasedKeyGenerator implements S3KeyGenerator {

        private String metaKey;

        public MetaBasedKeyGenerator(String metaKey) {
            this.metaKey = metaKey;
        }

        private String getPrefix(Map<String, MessageAttributeValue> attribsMap) {
            if (attribsMap.containsKey(metaKey))
                return attribsMap.get(metaKey).getStringValue();
            else
                return "NOT_SPECIFIED";
        }

        @Override
        public String generateObjectKey(SendMessageRequest sendMessageRequest) {
            return getPrefix(sendMessageRequest.getMessageAttributes()) + "/" + UUID.randomUUID().toString();
        }

        @Override
        public String generateObjectKey(SendMessageBatchRequestEntry batchEntry) {
            return getPrefix(batchEntry.getMessageAttributes()) + "/" + UUID.randomUUID().toString();
        }
    }

    static final String EVENT_TYPE = "EVENT_TYPE";

    @Before
    public void setupClient() {

        s3 = new AmazonS3Client(AWS_CREDS);
        s3.setEndpoint("s3-eu-west-1.amazonaws.com");

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME)
                //.withS3KeyGenerator(new DateKeyGenerator());
                .withS3KeyGenerator(new MetaBasedKeyGenerator(EVENT_TYPE))
                .withRetainS3Messages(true);

        client = new AmazonSQSClient(AWS_CREDS); //mock(AmazonSQSClient.class)
        sqs = new AmazonSQSExtendedClient(client, extendedClientConfiguration);
    }

    /* UNCOMMENT THIS TEST ONCE YOU HAVE FILLED IN YOUR AWS DETAILS IN THE STATIC VARS ABOVE

    @Test
    public void exercise() {

        int messageLength = SQS_SIZE_LIMIT + 1;
        String messageBody = generateString(messageLength);

        MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
        messageAttributeValue.setDataType("String");
        messageAttributeValue.setStringValue("Payment_Success");

        Map<String,MessageAttributeValue> metaData = new HashMap<String,MessageAttributeValue>();
        metaData.put(EVENT_TYPE, messageAttributeValue);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody)
                .withMessageAttributes(metaData);

        SendMessageResult sendResult = sqs.sendMessage(messageRequest);
        System.out.println(sendResult);

        ReceiveMessageRequest requestMsg = new ReceiveMessageRequest(SQS_QUEUE_URL).withMaxNumberOfMessages(1);

        ReceiveMessageResult receiveResult = sqs.receiveMessage(requestMsg);
        //System.out.println(receiveResult);

        DeleteMessageRequest deleteRequest = new DeleteMessageRequest(SQS_QUEUE_URL, receiveResult.getMessages().get(0).getReceiptHandle());

        sqs.deleteMessage(deleteRequest);
    }*/


	private String generateString(int messageLength) {
		char[] charArray = new char[messageLength];
		Arrays.fill(charArray, 'x');
		return new String(charArray);
	}
}
