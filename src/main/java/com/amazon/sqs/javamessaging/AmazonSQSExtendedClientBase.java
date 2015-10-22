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

import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.AddPermissionRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesResult;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.RemovePermissionRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

/**
 * This class provides a base for AmazonSQSExtendedClient class. It appends the
 * user agent. It also favors composition over inheritance which improves
 * testability.
 */
abstract class AmazonSQSExtendedClientBase implements AmazonSQS {
	AmazonSQS amazonSqsToBeExtended;

	public AmazonSQSExtendedClientBase() {
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setUserAgent(clientConfig.getUserAgent() + SQSExtendedClientConstants.USER_AGENT_HEADER);
		amazonSqsToBeExtended = new AmazonSQSClient(clientConfig);
	}

	public AmazonSQSExtendedClientBase(AWSCredentials awsCredentials) {
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setUserAgent(clientConfig.getUserAgent() + SQSExtendedClientConstants.USER_AGENT_HEADER);
		amazonSqsToBeExtended = new AmazonSQSClient(awsCredentials, clientConfig);
	}

	public AmazonSQSExtendedClientBase(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
		clientConfiguration.setUserAgent(clientConfiguration.getUserAgent()
				+ SQSExtendedClientConstants.USER_AGENT_HEADER);
		amazonSqsToBeExtended = new AmazonSQSClient(awsCredentials, clientConfiguration);
	}

	public AmazonSQSExtendedClientBase(AWSCredentialsProvider awsCredentialsProvider) {
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setUserAgent(clientConfig.getUserAgent() + SQSExtendedClientConstants.USER_AGENT_HEADER);
		amazonSqsToBeExtended = new AmazonSQSClient(awsCredentialsProvider, clientConfig);
	}

	public AmazonSQSExtendedClientBase(AWSCredentialsProvider awsCredentialsProvider,
			ClientConfiguration clientConfiguration) {
		clientConfiguration.setUserAgent(clientConfiguration.getUserAgent()
				+ SQSExtendedClientConstants.USER_AGENT_HEADER);
		amazonSqsToBeExtended = new AmazonSQSClient(awsCredentialsProvider, clientConfiguration);
	}

	public AmazonSQSExtendedClientBase(AWSCredentialsProvider awsCredentialsProvider,
			ClientConfiguration clientConfiguration, RequestMetricCollector requestMetricCollector) {
		clientConfiguration.setUserAgent(clientConfiguration.getUserAgent()
				+ SQSExtendedClientConstants.USER_AGENT_HEADER);
		amazonSqsToBeExtended = new AmazonSQSClient(awsCredentialsProvider, clientConfiguration, requestMetricCollector);
	}

	public AmazonSQSExtendedClientBase(ClientConfiguration clientConfiguration) {
		clientConfiguration.setUserAgent(clientConfiguration.getUserAgent()
				+ SQSExtendedClientConstants.USER_AGENT_HEADER);
		amazonSqsToBeExtended = new AmazonSQSClient(clientConfiguration);
	}

	public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
		return amazonSqsToBeExtended.sendMessage(sendMessageRequest);
	}

	public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
		return amazonSqsToBeExtended.receiveMessage(receiveMessageRequest);
	}

	public void deleteMessage(DeleteMessageRequest deleteMessageRequest) {
		amazonSqsToBeExtended.deleteMessage(deleteMessageRequest);
	}

	public void setEndpoint(String endpoint) throws IllegalArgumentException {

		amazonSqsToBeExtended.setEndpoint(endpoint);

	}

	public void setRegion(Region region) throws IllegalArgumentException {

		amazonSqsToBeExtended.setRegion(region);

	}

	public void setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) throws AmazonServiceException,
			AmazonClientException {

		amazonSqsToBeExtended.setQueueAttributes(setQueueAttributesRequest);

	}

	public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(
			ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) throws AmazonServiceException,
			AmazonClientException {

		return amazonSqsToBeExtended.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
	}

	public void changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
			throws AmazonServiceException, AmazonClientException {

		amazonSqsToBeExtended.changeMessageVisibility(changeMessageVisibilityRequest);
	}

	public GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) throws AmazonServiceException,
			AmazonClientException {

		return amazonSqsToBeExtended.getQueueUrl(getQueueUrlRequest);
	}

	public void removePermission(RemovePermissionRequest removePermissionRequest) throws AmazonServiceException,
			AmazonClientException {

		amazonSqsToBeExtended.removePermission(removePermissionRequest);
	}

	public GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest)
			throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.getQueueAttributes(getQueueAttributesRequest);
	}

	public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest)
			throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.sendMessageBatch(sendMessageBatchRequest);
	}

	public void purgeQueue(PurgeQueueRequest purgeQueueRequest) throws AmazonServiceException, AmazonClientException {

		amazonSqsToBeExtended.purgeQueue(purgeQueueRequest);

	}

	public ListDeadLetterSourceQueuesResult listDeadLetterSourceQueues(
			ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) throws AmazonServiceException,
			AmazonClientException {

		return amazonSqsToBeExtended.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
	}

	public void deleteQueue(DeleteQueueRequest deleteQueueRequest) throws AmazonServiceException, AmazonClientException {

		amazonSqsToBeExtended.deleteQueue(deleteQueueRequest);
	}

	public ListQueuesResult listQueues(ListQueuesRequest listQueuesRequest) throws AmazonServiceException,
			AmazonClientException {

		return amazonSqsToBeExtended.listQueues(listQueuesRequest);
	}

	public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest)
			throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.deleteMessageBatch(deleteMessageBatchRequest);
	}

	public CreateQueueResult createQueue(CreateQueueRequest createQueueRequest) throws AmazonServiceException,
			AmazonClientException {

		return amazonSqsToBeExtended.createQueue(createQueueRequest);
	}

	public void addPermission(AddPermissionRequest addPermissionRequest) throws AmazonServiceException,
			AmazonClientException {

		amazonSqsToBeExtended.addPermission(addPermissionRequest);
	}

	public ListQueuesResult listQueues() throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.listQueues();
	}

	public void setQueueAttributes(String queueUrl, Map<String, String> attributes) throws AmazonServiceException,
			AmazonClientException {

		amazonSqsToBeExtended.setQueueAttributes(queueUrl, attributes);
	}

	public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(String queueUrl,
			List<ChangeMessageVisibilityBatchRequestEntry> entries) throws AmazonServiceException,
			AmazonClientException {

		return amazonSqsToBeExtended.changeMessageVisibilityBatch(queueUrl, entries);
	}

	public void changeMessageVisibility(String queueUrl, String receiptHandle, Integer visibilityTimeout)
			throws AmazonServiceException, AmazonClientException {

		amazonSqsToBeExtended.changeMessageVisibility(queueUrl, receiptHandle, visibilityTimeout);
	}

	public GetQueueUrlResult getQueueUrl(String queueName) throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.getQueueUrl(queueName);
	}

	public void removePermission(String queueUrl, String label) throws AmazonServiceException, AmazonClientException {

		amazonSqsToBeExtended.removePermission(queueUrl, label);
	}

	public GetQueueAttributesResult getQueueAttributes(String queueUrl, List<String> attributeNames)
			throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.getQueueAttributes(queueUrl, attributeNames);
	}

	public SendMessageBatchResult sendMessageBatch(String queueUrl, List<SendMessageBatchRequestEntry> entries)
			throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.sendMessageBatch(queueUrl, entries);
	}

	public void deleteQueue(String queueUrl) throws AmazonServiceException, AmazonClientException {

		amazonSqsToBeExtended.deleteQueue(queueUrl);
	}

	public SendMessageResult sendMessage(String queueUrl, String messageBody) throws AmazonServiceException,
			AmazonClientException {

		return amazonSqsToBeExtended.sendMessage(queueUrl, messageBody);
	}

	public ReceiveMessageResult receiveMessage(String queueUrl) throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.receiveMessage(queueUrl);
	}

	public ListQueuesResult listQueues(String queueNamePrefix) throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.listQueues(queueNamePrefix);
	}

	public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries)
			throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.deleteMessageBatch(queueUrl, entries);
	}

	public CreateQueueResult createQueue(String queueName) throws AmazonServiceException, AmazonClientException {

		return amazonSqsToBeExtended.createQueue(queueName);
	}

	public void addPermission(String queueUrl, String label, List<String> aWSAccountIds, List<String> actions)
			throws AmazonServiceException, AmazonClientException {

		amazonSqsToBeExtended.addPermission(queueUrl, label, aWSAccountIds, actions);
	}

	public void deleteMessage(String queueUrl, String receiptHandle) throws AmazonServiceException,
			AmazonClientException {

		amazonSqsToBeExtended.deleteMessage(queueUrl, receiptHandle);
	}

	public void shutdown() {

		amazonSqsToBeExtended.shutdown();
	}

	public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {

		return amazonSqsToBeExtended.getCachedResponseMetadata(request);
	}

}
