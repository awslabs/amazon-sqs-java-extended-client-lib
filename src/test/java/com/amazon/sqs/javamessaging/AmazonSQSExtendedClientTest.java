/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClientBase;
import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.SQSExtended.AmazonSQSExtended;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
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

public class AmazonSQSExtendedClientTest {

	private static AmazonSQSExtended sqs;
	private static AmazonS3 s3;
	private static String s3BucketName = "test-bucket-name";
	private static String sqsQueueUrl = "test-queue-url";
	private static final int sqsSizeLimit = 262144;

	@Before
	public void setup() {
		sqs = spy(new AmazonSQSExtendedClient());
		((AmazonSQSExtendedClientBase) sqs).amazonSqsToBeExtended = mock(AmazonSQSClient.class);

		s3 = mock(AmazonS3.class);
		when(s3.doesBucketExist(s3BucketName)).thenReturn(true);
		when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

		sqs.enableLargePayloadSupport(s3, s3BucketName);
		verify(s3, times(1)).doesBucketExist(s3BucketName);
	}

	@Test
	public void testSendLargeMessage() {
		int messageLength = sqsSizeLimit + 1;
		String messageBody = generateString(messageLength);

		SendMessageRequest messageRequest = new SendMessageRequest(sqsQueueUrl, messageBody);
		sqs.sendMessage(messageRequest);
		verify(s3, times(1)).putObject(isA(PutObjectRequest.class));
	}

	@Test
	public void testSendSmallMessage() {
		int messageLength = sqsSizeLimit;
		String messageBody = generateString(messageLength);

		SendMessageRequest messageRequest = new SendMessageRequest(sqsQueueUrl, messageBody);
		sqs.sendMessage(messageRequest);
		verify(s3, never()).putObject(isA(PutObjectRequest.class));
	}

	@Test
	public void testDisableLargePayloadSupport() {
		int messageLength = 300000;
		String messageBody = generateString(messageLength);

		sqs.disableLargePayloadSupport();

		SendMessageRequest messageRequest = new SendMessageRequest(sqsQueueUrl, messageBody);
		sqs.sendMessage(messageRequest);
		verify(s3, never()).putObject(isA(PutObjectRequest.class));
	}

	@Test
	public void testSetAlwaysThroughS3() {
		int messageLength = 3;
		String messageBody = generateString(messageLength);

		sqs.setAlwaysThroughS3(true);

		SendMessageRequest messageRequest = new SendMessageRequest(sqsQueueUrl, messageBody);
		sqs.sendMessage(messageRequest);
		verify(s3, times(1)).putObject(isA(PutObjectRequest.class));
	}

	@Test
	public void testSetThreshold() {
		int messageLength = 1000;
		String messageBody = generateString(messageLength);

		sqs.setMessageSizeThreshold(500);

		SendMessageRequest messageRequest = new SendMessageRequest(sqsQueueUrl, messageBody);
		sqs.sendMessage(messageRequest);
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

		SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(sqsQueueUrl, batchEntries);
		sqs.sendMessageBatch(batchRequest);
		verify(s3, times(8)).putObject(isA(PutObjectRequest.class));
	}

	private String generateString(int messageLength) {
		char[] charArray = new char[messageLength];
		Arrays.fill(charArray, 'x');
		return new String(charArray);
	}

}
