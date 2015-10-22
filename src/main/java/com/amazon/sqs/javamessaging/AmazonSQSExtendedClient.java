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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import com.amazon.sqs.javamessaging.SQSExtendedClientConstants;
import com.amazon.sqs.javamessaging.SQSExtended.AmazonSQSExtended;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AmazonSQSExtendedClient extends AmazonSQSExtendedClientBase implements AmazonSQSExtended {
	private static final Log LOG = LogFactory.getLog(AmazonSQSExtendedClient.class);

	private AmazonS3 s3;
	private String s3BucketName;
	private boolean largePayloadSupport = false;
	private boolean alwaysThroughS3 = false;
	private int messageSizeThreshold = SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD;

	public AmazonSQSExtendedClient() {
		super();
	}

	public AmazonSQSExtendedClient(AWSCredentials awsCredentials) {
		super(awsCredentials);
	}

	public AmazonSQSExtendedClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
		super(awsCredentials, clientConfiguration);
	}

	public AmazonSQSExtendedClient(AWSCredentialsProvider awsCredentialsProvider) {
		super(awsCredentialsProvider);
	}

	public AmazonSQSExtendedClient(AWSCredentialsProvider awsCredentialsProvider,
			ClientConfiguration clientConfiguration) {
		super(awsCredentialsProvider, clientConfiguration);
	}

	public AmazonSQSExtendedClient(AWSCredentialsProvider awsCredentialsProvider,
			ClientConfiguration clientConfiguration, RequestMetricCollector requestMetricCollector) {
		super(awsCredentialsProvider, clientConfiguration, requestMetricCollector);
	}

	public AmazonSQSExtendedClient(ClientConfiguration clientConfiguration) {
		super(clientConfiguration);
	}

	public void enableLargePayloadSupport(AmazonS3 s3, String s3BucketName) {
		if (s3 == null || s3BucketName == null) {
			String errorMessage = "S3 client and/or S3 bucket name cannot be null.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}
		if (hasLargePayloadSupport()) {
			LOG.warn("Large-payload support is already enabled. Overwriting AmazonS3Client and S3BucketName.");
		}
		boolean bucketExists = s3.doesBucketExist(s3BucketName);
		if (!bucketExists) {
			String errorMessage = "No bucket with the specified name [" + s3BucketName + "] exists.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}
		if (!bucketExpirationIsSet(s3, s3BucketName)) {
			LOG.warn("Object expiration rule is not set for bucket [" + s3BucketName
					+ "] in object lifecycle configuration.");
		}
		this.s3 = s3;
		this.s3BucketName = s3BucketName;
		largePayloadSupport = true;
		LOG.info("Large-payload support enabled.");
	}

	public void disableLargePayloadSupport() {
		if (!hasLargePayloadSupport()) {
			LOG.warn("Large-payload support is already disabled.");
		}
		this.s3 = null;
		this.s3BucketName = null;
		largePayloadSupport = false;
		LOG.info("Large-payload support disabled.");
	}

	public boolean hasLargePayloadSupport() {
		return largePayloadSupport;
	}

	public AmazonS3 getAmazonS3Client() {
		return s3;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setMessageSizeThreshold(int messageSizeThreshold) {
		this.messageSizeThreshold = messageSizeThreshold;
	}

	public int getMessageSizeThreshold() {
		return messageSizeThreshold;
	}

	public void setAlwaysThroughS3(boolean alwaysThroughS3) {
		this.alwaysThroughS3 = alwaysThroughS3;
	}

	public boolean isAlwaysThroughS3() {
		return alwaysThroughS3;
	}

	/**
	 * This method sends an SQS message and uploads the payload to S3 if
	 * necessary.
	 */
	public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
		if (!largePayloadSupport) {
			return super.sendMessage(sendMessageRequest);
		}
		if (sendMessageRequest == null) {
			String errorMessage = "sendMessageRequest cannot be null.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}
		if (sendMessageRequest.getMessageBody() == null || "".equals(sendMessageRequest.getMessageBody())) {
			String errorMessage = "messageBody cannot be null or empty.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}

		if (alwaysThroughS3 || isLarge(sendMessageRequest)) {
			sendMessageRequest = storeMessageInS3(sendMessageRequest);
		}
		return super.sendMessage(sendMessageRequest);
	}

	/**
	 * This method sends an SQS message and uploads the payload to S3 if
	 * necessary.
	 */
	public SendMessageResult sendMessage(String queueUrl, String messageBody) {
		SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, messageBody);
		return sendMessage(sendMessageRequest);
	}

	/**
	 * This method receives SQS messages and downloads the payloads from S3 if
	 * necessary.
	 */
	public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
		if (!largePayloadSupport) {
			return super.receiveMessage(receiveMessageRequest);
		}
		if (receiveMessageRequest == null) {
			String errorMessage = "receiveMessageRequest cannot be null.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}

		receiveMessageRequest.getMessageAttributeNames().add(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);

		ReceiveMessageResult receiveMessageResult = super.receiveMessage(receiveMessageRequest);

		List<Message> messages = receiveMessageResult.getMessages();
		for (Message message : messages) {

			// for each received message check if they are stored in S3.
			MessageAttributeValue largePayloadAttributeValue = message.getMessageAttributes().get(
					SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
			if (largePayloadAttributeValue != null) {
				String messageBody = message.getBody();

				// read the S3 pointer from the message body JSON string.
				MessageS3Pointer s3Pointer = readMessageS3PointerFromJSON(messageBody);

				String s3MsgBucketName = s3Pointer.getS3BucketName();
				String s3MsgKey = s3Pointer.getS3Key();

				String origMsgBody = getTextFromS3(s3MsgBucketName, s3MsgKey);
				LOG.info("S3 object read, Bucket name: " + s3MsgBucketName + ", Object key: " + s3MsgKey + ".");

				message.setBody(origMsgBody);

				// remove the additional attribute before returning the message
				// to user.
				message.getMessageAttributes().remove(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);

				// Embed s3 object pointer in the receipt handle.
				String modifiedReceiptHandle = embedS3PointerInReceiptHandle(message.getReceiptHandle(),
						s3MsgBucketName, s3MsgKey);

				message.setReceiptHandle(modifiedReceiptHandle);
			}
		}
		return receiveMessageResult;
	}

	/**
	 * This method receives SQS messages and downloads the payloads from S3 if
	 * necessary.
	 */
	public ReceiveMessageResult receiveMessage(String queueUrl) {
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
		return receiveMessage(receiveMessageRequest);
	}

	/**
	 * This method deletes an SQS message and deletes the payload from S3 if
	 * necessary.
	 */
	public void deleteMessage(DeleteMessageRequest deleteMessageRequest) {
		if (!largePayloadSupport) {
			super.deleteMessage(deleteMessageRequest);
			return;
		}
		if (deleteMessageRequest == null) {
			String errorMessage = "deleteMessageRequest cannot be null.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}
		String receiptHandle = deleteMessageRequest.getReceiptHandle();
		String origReceiptHandle = receiptHandle;
		if (isS3ReceiptHandle(receiptHandle)) {
			deleteMessagePayloadFromS3(receiptHandle);
			origReceiptHandle = getOrigReceiptHandle(receiptHandle);
		}
		deleteMessageRequest.setReceiptHandle(origReceiptHandle);
		super.deleteMessage(deleteMessageRequest);
	}

	/**
	 * This method deletes an SQS message and deletes the payload from S3 if
	 * necessary.
	 */
	public void deleteMessage(String queueUrl, String receiptHandle) {
		DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(queueUrl, receiptHandle);
		deleteMessage(deleteMessageRequest);
	}

	/**
	 * This method sends a batch of SQS messages and uploads their payloads to
	 * S3 if necessary.
	 */
	public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
		if (!largePayloadSupport) {
			return super.sendMessageBatch(sendMessageBatchRequest);
		}
		if (sendMessageBatchRequest == null) {
			String errorMessage = "sendMessageBatchRequest cannot be null.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}

		List<SendMessageBatchRequestEntry> batchEntries = sendMessageBatchRequest.getEntries();

		for (int i = 0; i < batchEntries.size(); i++) {
			SendMessageBatchRequestEntry entry = batchEntries.get(i);
			if (alwaysThroughS3 || isLarge(entry)) {
				batchEntries.set(i, storeMessageInS3(entry));
			}
		}
		return super.sendMessageBatch(sendMessageBatchRequest);
	}

	/**
	 * This method sends a batch of SQS messages and uploads their payloads to
	 * S3 if necessary.
	 */
	public SendMessageBatchResult sendMessageBatch(String queueUrl, List<SendMessageBatchRequestEntry> entries) {
		SendMessageBatchRequest sendMessageBatchRequest = new SendMessageBatchRequest(queueUrl, entries);
		return sendMessageBatch(sendMessageBatchRequest);
	}

	/**
	 * This method deletes a batch of SQS messages and deletes their payloads
	 * from S3 if necessary.
	 */
	public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {
		if (!largePayloadSupport) {
			return super.deleteMessageBatch(deleteMessageBatchRequest);
		}
		if (deleteMessageBatchRequest == null) {
			String errorMessage = "deleteMessageBatchRequest cannot be null.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}
		for (DeleteMessageBatchRequestEntry entry : deleteMessageBatchRequest.getEntries()) {
			String receiptHandle = entry.getReceiptHandle();
			String origReceiptHandle = receiptHandle;
			if (isS3ReceiptHandle(receiptHandle)) {
				deleteMessagePayloadFromS3(receiptHandle);
				origReceiptHandle = getOrigReceiptHandle(receiptHandle);
			}
			entry.setReceiptHandle(origReceiptHandle);
		}
		return super.deleteMessageBatch(deleteMessageBatchRequest);
	}

	/**
	 * This method deletes a batch of SQS messages and deletes their payloads
	 * from S3 if necessary.
	 */
	public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries) {
		DeleteMessageBatchRequest deleteMessageBatchRequest = new DeleteMessageBatchRequest(queueUrl, entries);
		return deleteMessageBatch(deleteMessageBatchRequest);
	}

	public void purgeQueue(PurgeQueueRequest purgeQueueRequest) throws AmazonServiceException, AmazonClientException {
		LOG.warn("Calling purgeQueue deletes SQS messages without deleting their payload from S3.");
		super.purgeQueue(purgeQueueRequest);
	}

	private void deleteMessagePayloadFromS3(String receiptHandle) {
		String s3MsgBucketName = getFromReceiptHandleByMarker(receiptHandle,
				SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER);
		String s3MsgKey = getFromReceiptHandleByMarker(receiptHandle, SQSExtendedClientConstants.S3_KEY_MARKER);
		try {
			s3.deleteObject(s3MsgBucketName, s3MsgKey);
		} catch (AmazonServiceException e) {
			String errorMessage = "Failed to delete the S3 object which contains the SQS message payload. SQS message was not deleted.";
			LOG.error(errorMessage, e);
			throw new AmazonServiceException(errorMessage, e);
		} catch (AmazonClientException e) {
			String errorMessage = "Failed to delete the S3 object which contains the SQS message payload. SQS message was not deleted.";
			LOG.error(errorMessage, e);
			throw new AmazonClientException(errorMessage, e);
		}
		LOG.info("S3 object deleted, Bucket name: " + s3MsgBucketName + ", Object key: " + s3MsgKey + ".");
	}

	private void checkMessageAttributes(Map<String, MessageAttributeValue> messageAttributes) {
		int msgAttributesSize = getMsgAttributesSize(messageAttributes);
		if (msgAttributesSize > messageSizeThreshold) {
			String errorMessage = "Total size of Message attributes is " + msgAttributesSize
					+ " bytes which is larger than the threshold of " + messageSizeThreshold
					+ " Bytes. Consider including the payload in the message body instead of message attributes.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}

		int messageAttributesNum = messageAttributes.size();
		if (messageAttributesNum > SQSExtendedClientConstants.MAX_ALLOWED_ATTRIBUTES) {
			String errorMessage = "Number of message attributes [" + messageAttributesNum
					+ "] exceeds the maximum allowed for large-payload messages ["
					+ SQSExtendedClientConstants.MAX_ALLOWED_ATTRIBUTES + "].";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}

		MessageAttributeValue largePayloadAttributeValue = messageAttributes
				.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
		if (largePayloadAttributeValue != null) {
			String errorMessage = "Message attribute name " + SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME
					+ " is reserved for use by SQS extended client.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}

	}

	private String embedS3PointerInReceiptHandle(String receiptHandle, String s3MsgBucketName, String s3MsgKey) {
		String modifiedReceiptHandle = SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + s3MsgBucketName
				+ SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + SQSExtendedClientConstants.S3_KEY_MARKER
				+ s3MsgKey + SQSExtendedClientConstants.S3_KEY_MARKER + receiptHandle;
		return modifiedReceiptHandle;
	}

	private MessageS3Pointer readMessageS3PointerFromJSON(String messageBody) {

		MessageS3Pointer s3Pointer = null;
		try {
			JsonDataConverter jsonDataConverter = new JsonDataConverter();
			s3Pointer = jsonDataConverter.deserializeFromJson(messageBody, MessageS3Pointer.class);
		} catch (Exception e) {
			String errorMessage = "Failed to read the S3 object pointer from an SQS message. Message was not received.";
			LOG.error(errorMessage, e);
			throw new AmazonClientException(errorMessage, e);
		}
		return s3Pointer;
	}

	private String getOrigReceiptHandle(String receiptHandle) {
		int secondOccurence = receiptHandle.indexOf(SQSExtendedClientConstants.S3_KEY_MARKER,
				receiptHandle.indexOf(SQSExtendedClientConstants.S3_KEY_MARKER) + 1);
		return receiptHandle.substring(secondOccurence + SQSExtendedClientConstants.S3_KEY_MARKER.length());
	}

	private String getFromReceiptHandleByMarker(String receiptHandle, String marker) {
		int firstOccurence = receiptHandle.indexOf(marker);
		int secondOccurence = receiptHandle.indexOf(marker, firstOccurence + 1);
		return receiptHandle.substring(firstOccurence + marker.length(), secondOccurence);
	}

	private boolean isS3ReceiptHandle(String receiptHandle) {
		return receiptHandle.contains(SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER)
				&& receiptHandle.contains(SQSExtendedClientConstants.S3_KEY_MARKER);
	}

	private String getTextFromS3(String s3BucketName, String s3Key) {
		GetObjectRequest getObjectRequest = new GetObjectRequest(s3BucketName, s3Key);
		String embeddedText = null;
		S3Object obj = null;
		try {
			obj = s3.getObject(getObjectRequest);
		} catch (AmazonServiceException e) {
			String errorMessage = "Failed to get the S3 object which contains the message payload. Message was not received.";
			LOG.error(errorMessage, e);
			throw new AmazonServiceException(errorMessage, e);
		} catch (AmazonClientException e) {
			String errorMessage = "Failed to get the S3 object which contains the message payload. Message was not received.";
			LOG.error(errorMessage, e);
			throw new AmazonClientException(errorMessage, e);
		}
		try {
			InputStream objContent = obj.getObjectContent();
			java.util.Scanner objContentScanner = new java.util.Scanner(objContent, "UTF-8");
			objContentScanner.useDelimiter("\\A");
			embeddedText = objContentScanner.hasNext() ? objContentScanner.next() : "";
			objContentScanner.close();
			objContent.close();
		} catch (IOException e) {
			String errorMessage = "Failure when handling the message which was read from S3 object. Message was not received.";
			LOG.error(errorMessage, e);
			throw new AmazonClientException(errorMessage, e);
		}
		return embeddedText;
	}

	private boolean isLarge(SendMessageRequest sendMessageRequest) {
		int msgAttributesSize = getMsgAttributesSize(sendMessageRequest.getMessageAttributes());
		long msgBodySize = getStringSizeInBytes(sendMessageRequest.getMessageBody());
		long totalMsgSize = msgAttributesSize + msgBodySize;
		return (totalMsgSize > messageSizeThreshold);
	}

	private boolean isLarge(SendMessageBatchRequestEntry batchEntry) {
		int msgAttributesSize = getMsgAttributesSize(batchEntry.getMessageAttributes());
		long msgBodySize = getStringSizeInBytes(batchEntry.getMessageBody());
		long totalMsgSize = msgAttributesSize + msgBodySize;
		return (totalMsgSize > messageSizeThreshold);
	}

	private int getMsgAttributesSize(Map<String, MessageAttributeValue> msgAttributes) {
		int totalMsgAttributesSize = 0;
		for (Entry<String, MessageAttributeValue> entry : msgAttributes.entrySet()) {
			totalMsgAttributesSize += getStringSizeInBytes(entry.getKey());

			MessageAttributeValue entryVal = entry.getValue();
			if (entryVal.getDataType() != null) {
				totalMsgAttributesSize += getStringSizeInBytes(entryVal.getDataType());
			}

			String stringVal = entryVal.getStringValue();
			if (stringVal != null) {
				totalMsgAttributesSize += getStringSizeInBytes(entryVal.getStringValue());
			}

			ByteBuffer binaryVal = entryVal.getBinaryValue();
			if (binaryVal != null) {
				totalMsgAttributesSize += binaryVal.array().length;
			}
		}
		return totalMsgAttributesSize;
	}

	private SendMessageBatchRequestEntry storeMessageInS3(SendMessageBatchRequestEntry batchEntry) {

		checkMessageAttributes(batchEntry.getMessageAttributes());

		String s3Key = UUID.randomUUID().toString();

		// Read the content of the message from message body
		String messageContentStr = batchEntry.getMessageBody();

		Long messageContentSize = getStringSizeInBytes(messageContentStr);

		// Add a new message attribute as a flag
		MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
		messageAttributeValue.setDataType("Number");
		messageAttributeValue.setStringValue(messageContentSize.toString());
		batchEntry.addMessageAttributesEntry(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME, messageAttributeValue);

		// Store the message content in S3.
		storeTextInS3(s3Key, messageContentStr, messageContentSize);

		LOG.info("S3 object created, Bucket name: " + s3BucketName + ", Object key: " + s3Key + ".");

		// Convert S3 pointer (bucket name, key, etc) to JSON string
		MessageS3Pointer s3Pointer = new MessageS3Pointer(s3BucketName, s3Key);
		String s3PointerStr = getJSONFromS3Pointer(s3Pointer);

		// Storing S3 pointer in the message body.
		batchEntry.setMessageBody(s3PointerStr);

		return batchEntry;
	}

	private SendMessageRequest storeMessageInS3(SendMessageRequest sendMessageRequest) {

		checkMessageAttributes(sendMessageRequest.getMessageAttributes());

		String s3Key = UUID.randomUUID().toString();

		// Read the content of the message from message body
		String messageContentStr = sendMessageRequest.getMessageBody();

		Long messageContentSize = getStringSizeInBytes(messageContentStr);

		// Add a new message attribute as a flag
		MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
		messageAttributeValue.setDataType("Number");
		messageAttributeValue.setStringValue(messageContentSize.toString());
		sendMessageRequest.addMessageAttributesEntry(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME,
				messageAttributeValue);

		// Store the message content in S3.
		storeTextInS3(s3Key, messageContentStr, messageContentSize);
		LOG.info("S3 object created, Bucket name: " + s3BucketName + ", Object key: " + s3Key + ".");

		// Convert S3 pointer (bucket name, key, etc) to JSON string
		MessageS3Pointer s3Pointer = new MessageS3Pointer(s3BucketName, s3Key);

		String s3PointerStr = getJSONFromS3Pointer(s3Pointer);

		// Storing S3 pointer in the message body.
		sendMessageRequest.setMessageBody(s3PointerStr);

		return sendMessageRequest;
	}

	private String getJSONFromS3Pointer(MessageS3Pointer s3Pointer) {
		String s3PointerStr = null;
		try {
			JsonDataConverter jsonDataConverter = new JsonDataConverter();
			s3PointerStr = jsonDataConverter.serializeToJson(s3Pointer);
		} catch (Exception e) {
			String errorMessage = "Failed to convert S3 object pointer to text. Message was not sent.";
			LOG.error(errorMessage, e);
			throw new AmazonClientException(errorMessage, e);
		}
		return s3PointerStr;
	}

	private void storeTextInS3(String s3Key, String messageContentStr, Long messageContentSize) {
		InputStream messageContentStream = new ByteArrayInputStream(messageContentStr.getBytes(StandardCharsets.UTF_8));
		ObjectMetadata messageContentStreamMetadata = new ObjectMetadata();
		messageContentStreamMetadata.setContentLength(messageContentSize);
		PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, s3Key, messageContentStream,
				messageContentStreamMetadata);
		try {
			s3.putObject(putObjectRequest);
		} catch (AmazonServiceException e) {
			String errorMessage = "Failed to store the message content in an S3 object. SQS message was not sent.";
			LOG.error(errorMessage, e);
			throw new AmazonServiceException(errorMessage, e);
		} catch (AmazonClientException e) {
			String errorMessage = "Failed to store the message content in an S3 object. SQS message was not sent.";
			LOG.error(errorMessage, e);
			throw new AmazonClientException(errorMessage, e);
		}
	}

	private static long getStringSizeInBytes(String str) {
		CountingOutputStream counterOutputStream = new CountingOutputStream();
		try {
			Writer writer = new OutputStreamWriter(counterOutputStream, "UTF-8");
			writer.write(str);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			String errorMessage = "Failed to calculate the size of message payload.";
			LOG.error(errorMessage, e);
			throw new AmazonClientException(errorMessage, e);
		}
		return counterOutputStream.getTotalSize();
	}

	private boolean bucketExpirationIsSet(AmazonS3 s3Client, String s3ClientBucketName) {
		boolean bucketExpirationIsSet = false;
		try {
			BucketLifecycleConfiguration bucketLifeCycleConfig = s3Client
					.getBucketLifecycleConfiguration(s3ClientBucketName);
			List<BucketLifecycleConfiguration.Rule> bucketRulesList = bucketLifeCycleConfig.getRules();
			for (BucketLifecycleConfiguration.Rule rule : bucketRulesList) {
				if ((rule.getStatus() != null && rule.getStatus().equals(BucketLifecycleConfiguration.ENABLED))
						&& (rule.getPrefix() == null || "".equals(rule.getPrefix()))
						&& (rule.getExpirationDate() != null || rule.getExpirationInDays() > 0)) {
					bucketExpirationIsSet = true;
				}
			}
		} catch (Exception e) {
		}
		return bucketExpirationIsSet;
	}

}
