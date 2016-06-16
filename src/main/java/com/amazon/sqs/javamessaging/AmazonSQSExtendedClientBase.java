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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AddPermissionRequest;
import com.amazonaws.services.sqs.model.AddPermissionResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesResult;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.PurgeQueueResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.RemovePermissionRequest;
import com.amazonaws.services.sqs.model.RemovePermissionResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesResult;

import java.util.List;
import java.util.Map;

abstract class AmazonSQSExtendedClientBase implements AmazonSQS {
	private final AmazonSQS amazonSqsToBeExtended;

	AmazonSQSExtendedClientBase(AmazonSQS sqsClient) {
		amazonSqsToBeExtended = sqsClient;
	}

	/**
	 * {@inheritDoc}
	 */
	public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
		return amazonSqsToBeExtended.sendMessage(sendMessageRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
		return amazonSqsToBeExtended.receiveMessage(receiveMessageRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest) {
		return amazonSqsToBeExtended.deleteMessage(deleteMessageRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendMessageResult sendMessage(String queueUrl, String messageBody) throws AmazonClientException {
		return amazonSqsToBeExtended.sendMessage(queueUrl, messageBody);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceiveMessageResult receiveMessage(String queueUrl) throws AmazonClientException {
		return amazonSqsToBeExtended.receiveMessage(queueUrl);
	}

	/**
	 * {@inheritDoc}
	 */
	public DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle) throws AmazonClientException {
		return amazonSqsToBeExtended.deleteMessage(queueUrl, receiptHandle);
	}

	/**
	 * {@inheritDoc}
	 */
	public SetQueueAttributesResult setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) throws
			AmazonClientException {
		return amazonSqsToBeExtended.setQueueAttributes(setQueueAttributesRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(
			ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) throws AmazonClientException {
		return amazonSqsToBeExtended.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public ChangeMessageVisibilityResult changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
			throws AmazonClientException {
		return amazonSqsToBeExtended.changeMessageVisibility(changeMessageVisibilityRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest)
			throws AmazonClientException {
		return amazonSqsToBeExtended.getQueueUrl(getQueueUrlRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public RemovePermissionResult removePermission(RemovePermissionRequest removePermissionRequest)
			throws AmazonClientException {
		return amazonSqsToBeExtended.removePermission(removePermissionRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest)
			throws AmazonClientException {
		return amazonSqsToBeExtended.getQueueAttributes(getQueueAttributesRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest) throws AmazonClientException {
		return amazonSqsToBeExtended.purgeQueue(purgeQueueRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public ListDeadLetterSourceQueuesResult listDeadLetterSourceQueues(
			ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) throws
			AmazonClientException {
		return amazonSqsToBeExtended.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public DeleteQueueResult deleteQueue(DeleteQueueRequest deleteQueueRequest) throws AmazonClientException {
		return amazonSqsToBeExtended.deleteQueue(deleteQueueRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public ListQueuesResult listQueues(ListQueuesRequest listQueuesRequest) throws AmazonClientException {
		return amazonSqsToBeExtended.listQueues(listQueuesRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest)
			throws AmazonClientException {

		return amazonSqsToBeExtended.deleteMessageBatch(deleteMessageBatchRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public CreateQueueResult createQueue(CreateQueueRequest createQueueRequest) throws AmazonClientException {

		return amazonSqsToBeExtended.createQueue(createQueueRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public AddPermissionResult addPermission(AddPermissionRequest addPermissionRequest) throws AmazonClientException {
		return amazonSqsToBeExtended.addPermission(addPermissionRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public ListQueuesResult listQueues() throws AmazonClientException {
		return amazonSqsToBeExtended.listQueues();
	}

	/**
	 * {@inheritDoc}
	 */
	public SetQueueAttributesResult setQueueAttributes(String queueUrl, Map<String, String> attributes)
			throws AmazonClientException {
		return amazonSqsToBeExtended.setQueueAttributes(queueUrl, attributes);
	}

	/**
	 * {@inheritDoc}
	 */
	public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(String queueUrl,
			List<ChangeMessageVisibilityBatchRequestEntry> entries) throws AmazonClientException {
		return amazonSqsToBeExtended.changeMessageVisibilityBatch(queueUrl, entries);
	}

	/**
	 * {@inheritDoc}
	 */
	public ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl, String receiptHandle, Integer visibilityTimeout)
			throws AmazonClientException {
		return amazonSqsToBeExtended.changeMessageVisibility(queueUrl, receiptHandle, visibilityTimeout);
	}

	/**
	 * {@inheritDoc}
	 */
	public GetQueueUrlResult getQueueUrl(String queueName) throws AmazonClientException {
		return amazonSqsToBeExtended.getQueueUrl(queueName);
	}

	/**
	 * {@inheritDoc}
	 */
	public RemovePermissionResult removePermission(String queueUrl, String label) throws AmazonClientException {
		return amazonSqsToBeExtended.removePermission(queueUrl, label);
	}

	/**
	 * {@inheritDoc}
	 */
	public GetQueueAttributesResult getQueueAttributes(String queueUrl, List<String> attributeNames)
			throws AmazonClientException {
		return amazonSqsToBeExtended.getQueueAttributes(queueUrl, attributeNames);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendMessageBatchResult sendMessageBatch(String queueUrl, List<SendMessageBatchRequestEntry> entries)
			throws AmazonClientException {
		return amazonSqsToBeExtended.sendMessageBatch(queueUrl, entries);
	}

	/**
	 * {@inheritDoc}
	 */
	public DeleteQueueResult deleteQueue(String queueUrl) throws AmazonClientException {
		return amazonSqsToBeExtended.deleteQueue(queueUrl);
	}

	/**
	 * {@inheritDoc}
	 */
	public ListQueuesResult listQueues(String queueNamePrefix) throws AmazonClientException {

		return amazonSqsToBeExtended.listQueues(queueNamePrefix);
	}

	/**
	 * {@inheritDoc}
	 */
	public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries)
			throws AmazonClientException {
		return amazonSqsToBeExtended.deleteMessageBatch(queueUrl, entries);
	}

	/**
	 * {@inheritDoc}
	 */
	public CreateQueueResult createQueue(String queueName) throws AmazonClientException {
		return amazonSqsToBeExtended.createQueue(queueName);
	}

	/**
	 * {@inheritDoc}
	 */
	public AddPermissionResult addPermission(String queueUrl, String label, List<String> aWSAccountIds, List<String> actions)
			throws AmazonClientException {
		return amazonSqsToBeExtended.addPermission(queueUrl, label, aWSAccountIds, actions);
	}

	/**
	 * {@inheritDoc}
	 */
	public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
		return amazonSqsToBeExtended.getCachedResponseMetadata(request);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEndpoint(String endpoint) throws IllegalArgumentException {
		amazonSqsToBeExtended.setEndpoint(endpoint);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegion(Region region) throws IllegalArgumentException {
		amazonSqsToBeExtended.setRegion(region);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
		return amazonSqsToBeExtended.sendMessageBatch(sendMessageBatchRequest);
	}

	/**
	 * {@inheritDoc}
	 */
	public void shutdown() {
		amazonSqsToBeExtended.shutdown();
	}

}
