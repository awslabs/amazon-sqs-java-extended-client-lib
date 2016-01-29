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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import com.amazonaws.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.any;
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

	private AmazonSQS wrappedSQS;

	private AmazonS3 s3;

	private static final String S3_BUCKET_NAME = "test-bucket-name";

	private static final String SQS_QUEUE_URL = "test-queue-url";

	private static final int SQS_SIZE_LIMIT = 262144;

	public static final String MESSAGE_HANDLE = "MbZj6wDWli+JvwwJaBV+3dcjk2YW2vA3+STFFljTM8tJJg6HRG6PYSasuWXPJB+Cw\n"
																							+ "Lj1FjgXUv1uSj1gUPAWV66FU/WeR4mq2OKpEGYWbnLmpRCJVAyeMjeU5ZBdtcQ+QE\n"
																							+ "auMZc8ZRv37sIW2iJKq3M9MFx1YvV11A2x/KSbkJ0=";

	public static final String LARGE_MESSAGE_HANDLE =
					"-..s3BucketName..-test-bucket-name-..s3BucketName..--..s3Key..-7f096cb3-454d-4bd8-923d-8948a300f1c1-..s3Key..-"
					+ MESSAGE_HANDLE;

	@Before
	public void setupClient() {
		s3 = mock(AmazonS3.class);
		when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(new PutObjectResult());

		ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration().withLargePayloadSupportEnabled(s3,
																																																															 S3_BUCKET_NAME);
		wrappedSQS = mock(AmazonSQSClient.class);
		sqs = new AmazonSQSExtendedClient(wrappedSQS, extendedClientConfiguration);

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

		ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration().withLargePayloadSupportDisabled();

		AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

		SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
		sqsExtended.sendMessage(messageRequest);
		verify(s3, never()).putObject(isA(PutObjectRequest.class));
	}

	@Test
	public void testSendMessageWithAlwaysThroughS3() {
		int messageLength = 3;
		String messageBody = generateString(messageLength);

		ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration().withLargePayloadSupportEnabled(s3,
																																																															 S3_BUCKET_NAME)
																																															 .withAlwaysThroughS3(true);

		AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));


		SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
		sqsExtended.sendMessage(messageRequest);
		verify(s3, times(1)).putObject(isA(PutObjectRequest.class));
	}

	@Test
	public void testSendMessageWithSetMessageSizeThreshold() {
		int messageLength = 1000;
		String messageBody = generateString(messageLength);

		ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration().withLargePayloadSupportEnabled(s3,
																																																															 S3_BUCKET_NAME)
																																															 .withMessageSizeThreshold(500);

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

	@Test
	public void testReceiveLargeMessage() {

		int messageLength = 300000;
		String messageBody = generateString(messageLength);

		Message message = new Message();
		message.withBody("[\"com.amazon.sqs.javamessaging.MessageS3Pointer\"," + "{\"s3BucketName\":\"test-bucket-name\","
										 + "\"s3Key\":\"7f096cb3-454d-4bd8-923d-8948a300f1c1\",\"md5\":\"" +
										 Base64.encodeAsString(DigestUtils.md5(messageBody)) + "\"}]");
		message.addMessageAttributesEntry(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME,
																			new MessageAttributeValue().withDataType("Number")
																																 .withStringValue(Integer.toString(messageBody.getBytes(
																																				 StandardCharsets.UTF_8).length)));

		S3Object s3Body = new S3Object();
		s3Body.setObjectContent(new ByteArrayInputStream(messageBody.getBytes(StandardCharsets.UTF_8)));

		when(wrappedSQS.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult().withMessages(message));
		when(s3.getObject(any(GetObjectRequest.class))).thenReturn(s3Body);

		ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(SQS_QUEUE_URL);
		// aws request objects lack equals methods for matcher verification
		ArgumentCaptor<GetObjectRequest> getObjectArgument = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(wrappedSQS, times(1)).receiveMessage(isA(ReceiveMessageRequest.class));
		verify(s3, times(1)).getObject(getObjectArgument.capture());
		Assert.assertEquals("7f096cb3-454d-4bd8-923d-8948a300f1c1", getObjectArgument.getValue().getKey());
		Assert.assertEquals(receiveMessageResult.getMessages().size(), 1);
		Assert.assertEquals(messageBody, receiveMessageResult.getMessages().get(0).getBody());
	}

	@Test
	public void testReceiveSmallMessage() {
		Message message = new Message();
		message.withBody("not a large body");

		when(wrappedSQS.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult().withMessages(message));

		sqs.receiveMessage(SQS_QUEUE_URL);
		verify(wrappedSQS, times(1)).receiveMessage(isA(ReceiveMessageRequest.class));
		verify(s3, never()).getObject(any(GetObjectRequest.class));
	}

	@Test
	public void testDeleteLargeMessage() {

		sqs.deleteMessage(SQS_QUEUE_URL, LARGE_MESSAGE_HANDLE);
		// aws request objects lack equals methods for matcher verification
		ArgumentCaptor<DeleteObjectRequest> deleteObjectArgument = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		ArgumentCaptor<DeleteMessageRequest> deleteMessageArgument = ArgumentCaptor.forClass(DeleteMessageRequest.class);
		verify(s3, times(1)).deleteObject(deleteObjectArgument.capture());
		verify(wrappedSQS, times(1)).deleteMessage(deleteMessageArgument.capture());
		Assert.assertEquals("7f096cb3-454d-4bd8-923d-8948a300f1c1",
												deleteObjectArgument.getValue().getKey(),
												"7f096cb3-454d-4bd8-923d-8948a300f1c1");
		Assert.assertEquals(MESSAGE_HANDLE, deleteMessageArgument.getValue().getReceiptHandle());
	}

	@Test
	public void testDeleteSmallMessage() {
		sqs.deleteMessage(SQS_QUEUE_URL, MESSAGE_HANDLE);
		// aws request objects lack equals methods for matcher verification
		ArgumentCaptor<DeleteMessageRequest> deleteMessageArgument = ArgumentCaptor.forClass(DeleteMessageRequest.class);
		verify(s3, never()).deleteObject(isA(DeleteObjectRequest.class));
		verify(wrappedSQS, times(1)).deleteMessage(deleteMessageArgument.capture());
		Assert.assertEquals(MESSAGE_HANDLE, deleteMessageArgument.getValue().getReceiptHandle());
	}

	@Test
	public void testChangeMessageVisibilityLargeMessage() {
		sqs.changeMessageVisibility(SQS_QUEUE_URL, LARGE_MESSAGE_HANDLE, 10);
		// aws request objects lack equals methods for matcher verification
		ArgumentCaptor<ChangeMessageVisibilityRequest> changeVisibilityArgument = ArgumentCaptor.forClass(
						ChangeMessageVisibilityRequest.class);
		verify((wrappedSQS), times(1)).changeMessageVisibility(changeVisibilityArgument.capture());
		Assert.assertEquals(MESSAGE_HANDLE, changeVisibilityArgument.getValue().getReceiptHandle());
	}

	@Test
	public void testChangeMessageVisibilitySmallMessage() {
		String messageHandle = "MbZj6wDWli+JvwwJaBV+3dcjk2YW2vA3+STFFljTM8tJJg6HRG6PYSasuWXPJB+Cw\n"
													 + "Lj1FjgXUv1uSj1gUPAWV66FU/WeR4mq2OKpEGYWbnLmpRCJVAyeMjeU5ZBdtcQ+QE\n"
													 + "auMZc8ZRv37sIW2iJKq3M9MFx1YvV11A2x/KSbkJ0=";

		sqs.changeMessageVisibility(SQS_QUEUE_URL, messageHandle, 10);
		// aws request objects lack equals methods for matcher verification
		ArgumentCaptor<ChangeMessageVisibilityRequest> changeVisibilityArgument = ArgumentCaptor.forClass(
						ChangeMessageVisibilityRequest.class);
		verify(wrappedSQS, times(1)).changeMessageVisibility(changeVisibilityArgument.capture());
		Assert.assertEquals(messageHandle, changeVisibilityArgument.getValue().getReceiptHandle());
	}

	@Test
	public void testChangeMessageVisibilityBatch() {

		List<ChangeMessageVisibilityBatchRequestEntry> entries = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
		for (int k = 0; k < 3; k++) {
			entries.add(new ChangeMessageVisibilityBatchRequestEntry(Integer.toString(k), LARGE_MESSAGE_HANDLE));
		}
		ArgumentCaptor<ChangeMessageVisibilityBatchRequest> changeVisibilityArgument = ArgumentCaptor.forClass(
						ChangeMessageVisibilityBatchRequest.class);
		sqs.changeMessageVisibilityBatch(new ChangeMessageVisibilityBatchRequest(SQS_QUEUE_URL, entries));
		verify(wrappedSQS, times(1)).changeMessageVisibilityBatch(changeVisibilityArgument.capture());
		for (ChangeMessageVisibilityBatchRequestEntry entry : changeVisibilityArgument.getValue().getEntries()) {
			Assert.assertEquals(MESSAGE_HANDLE, entry.getReceiptHandle());
		}

	}

	private String generateString(int messageLength) {
		char[] charArray = new char[messageLength];
		Arrays.fill(charArray, 'x');
		return new String(charArray);
	}

}
