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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.isA;

/**
 * Tests the AmazonSQSExtendedClient class.
 */
public class AmazonSQSExtendedClientTest {

    private AmazonSQS sqs;
    private AmazonS3 s3;
    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String SQS_QUEUE_URL = "test-queue-url";
    private static final int SQS_SIZE_LIMIT = 262144;

    @Before
    public void setupClient() {
        s3 = mock(AmazonS3.class);
        when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME);

        sqs = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

    }

	@Test
	public void testSendLargeMessage() {

		int messageLength = SQS_SIZE_LIMIT + 1;
		String messageBody = generateString(messageLength);

		SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);

		sqs.sendMessage(messageRequest);
		verify(s3, times(1)).putObject(isA(PutObjectRequest.class));
	}

	@Test
	public void testSendSmallMessage() {
		int messageLength = SQS_SIZE_LIMIT;
		String messageBody = generateString(messageLength);

		SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
		sqs.sendMessage(messageRequest);
		verify(s3, never()).putObject(isA(PutObjectRequest.class));
	}

    @Test
    public void testSendMessageWithLargePayloadSupportDisabled() {
        int messageLength = 300000;
        String messageBody = generateString(messageLength);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportDisabled();

        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);
        verify(s3, never()).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testSendMessageWithAlwaysThroughS3() {
        int messageLength = 3;
        String messageBody = generateString(messageLength);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME).withAlwaysThroughS3(true);

        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));


        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);
        verify(s3, times(1)).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testSendMessageWithSetMessageSizeThreshold() {
        int messageLength = 1000;
        String messageBody = generateString(messageLength);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME).withMessageSizeThreshold(500);

        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);
        verify(s3, times(1)).putObject(isA(PutObjectRequest.class));
    }

	@Test
	public void testMessageBatch() {

		List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<SendMessageBatchRequestEntry>();

		for (int i = 1; i <= 10; i++) {
			SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
			int messageLength = i * 100000;
			String messageBody = generateString(messageLength);
			entry.setMessageBody(messageBody);
			entry.setId("entry_" + i);
			batchEntries.add(entry);
		}

		SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(SQS_QUEUE_URL, batchEntries);
        sqs.sendMessageBatch(batchRequest);
		verify(s3, times(8)).putObject(isA(PutObjectRequest.class));
	}

	private String generateString(int messageLength) {
		char[] charArray = new char[messageLength];
		Arrays.fill(charArray, 'x');
		return new String(charArray);
	}

}
