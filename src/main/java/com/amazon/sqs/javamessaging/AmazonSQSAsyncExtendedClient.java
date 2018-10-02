/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import com.amazonaws.AmazonClientException;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.PurgeQueueResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Amazon SQS Extended Client extends the functionality of Amazon SQS client.
 * All service calls made using this client are blocking, and will not return
 * until the service call completes.
 *
 * <p>
 * The Amazon SQS extended client enables sending and receiving large messages
 * via Amazon S3. You can use this library to:
 * </p>
 *
 * <ul>
 * <li>Specify whether messages are always stored in Amazon S3 or only when a
 * message size exceeds 256 KB.</li>
 * <li>Send a message that references a single message object stored in an
 * Amazon S3 bucket.</li>
 * <li>Get the corresponding message object from an Amazon S3 bucket.</li>
 * <li>Delete the corresponding message object from an Amazon S3 bucket.</li>
 * </ul>
 */
public class AmazonSQSAsyncExtendedClient extends AmazonSQSAsyncExtendedClientBase implements AmazonSQSAsync {
    private static final Log LOG = LogFactory.getLog(AmazonSQSAsyncExtendedClient.class);

    private ExtendedClientConfiguration clientConfiguration;
    private AmazonSQSExtendedClient synchClient;

    /**
     * Constructs a new Amazon SQS extended client to invoke service methods on
     * Amazon SQS with extended functionality using the specified Amazon SQS
     * client object.
     *
     * <p>
     * All service calls made using this new client object are blocking, and
     * will not return until the service call completes.
     *
     * @param sqsClient The Amazon SQS client to use to connect to Amazon SQS.
     */
    public AmazonSQSAsyncExtendedClient(AmazonSQSAsync sqsClient) {
        this(sqsClient, new ExtendedClientConfiguration());
    }

    /**
     * Constructs a new Amazon SQS extended client to invoke service methods on
     * Amazon SQS with extended functionality using the specified Amazon SQS
     * client object.
     *
     * <p>
     * All service calls made using this new client object are blocking, and
     * will not return until the service call completes.
     *
     * @param sqsClient            The Amazon SQS client to use to connect to Amazon SQS.
     * @param extendedClientConfig The extended client configuration options controlling the
     *                             functionality of this client.
     */
    public AmazonSQSAsyncExtendedClient(AmazonSQSAsync sqsClient, ExtendedClientConfiguration extendedClientConfig) {
        super(sqsClient);
        this.clientConfiguration = new ExtendedClientConfiguration(extendedClientConfig);
        this.synchClient = new AmazonSQSExtendedClient(sqsClient, extendedClientConfig);
    }

    @Override
    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {

        if (synchClient.isS3ReceiptHandle(changeMessageVisibilityRequest.getReceiptHandle())) {
            changeMessageVisibilityRequest.setReceiptHandle(
                    synchClient.getOrigReceiptHandle(changeMessageVisibilityRequest.getReceiptHandle()));
        }

        return amazonSqsToBeExtended.changeMessageVisibilityAsync(changeMessageVisibilityRequest);
    }

    @Override
    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(ChangeMessageVisibilityRequest changeMessageVisibilityRequest,
                                                                              AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> asyncHandler) {

        return amazonSqsToBeExtended.changeMessageVisibilityAsync(changeMessageVisibilityRequest, asyncHandler);
    }

    @Override
    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(String queueUrl,
                                                                              String receiptHandle,
                                                                              Integer visibilityTimeout) {

        ChangeMessageVisibilityRequest changeMessageVisibilityRequest =
                new ChangeMessageVisibilityRequest(queueUrl, receiptHandle, visibilityTimeout);
        return changeMessageVisibilityAsync(changeMessageVisibilityRequest);
    }

    @Override
    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(String queueUrl,
                                                                              String receiptHandle,
                                                                              Integer visibilityTimeout,
                                                                              AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> asyncHandler) {

        ChangeMessageVisibilityRequest changeMessageVisibilityRequest =
                new ChangeMessageVisibilityRequest(queueUrl, receiptHandle, visibilityTimeout);
        return changeMessageVisibilityAsync(changeMessageVisibilityRequest, asyncHandler);
    }

    @Override
    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {

        for (ChangeMessageVisibilityBatchRequestEntry entry : changeMessageVisibilityBatchRequest.getEntries()) {
            if (synchClient.isS3ReceiptHandle(entry.getReceiptHandle())) {
                entry.setReceiptHandle(synchClient.getOrigReceiptHandle(entry.getReceiptHandle()));
            }
        }

        return amazonSqsToBeExtended.changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest);
    }

    @Override
    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest,
            AsyncHandler<ChangeMessageVisibilityBatchRequest, ChangeMessageVisibilityBatchResult> asyncHandler) {

        for (ChangeMessageVisibilityBatchRequestEntry entry : changeMessageVisibilityBatchRequest.getEntries()) {
            if (synchClient.isS3ReceiptHandle(entry.getReceiptHandle())) {
                entry.setReceiptHandle(synchClient.getOrigReceiptHandle(entry.getReceiptHandle()));
            }
        }
        return amazonSqsToBeExtended.changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest, asyncHandler);
    }

    @Override
    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(String queueUrl,
                                                                                        List<ChangeMessageVisibilityBatchRequestEntry> entries) {
        ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest =
                new ChangeMessageVisibilityBatchRequest(queueUrl, entries);
        return changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest);
    }

    @Override
    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(String queueUrl,
                                                                                        List<ChangeMessageVisibilityBatchRequestEntry> entries,
                                                                                        AsyncHandler<ChangeMessageVisibilityBatchRequest, ChangeMessageVisibilityBatchResult> asyncHandler) {
        ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest =
                new ChangeMessageVisibilityBatchRequest(queueUrl, entries);
        return changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest, asyncHandler);
    }

    @Override
    public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest) {

        if (deleteMessageRequest == null) {
            String errorMessage = "deleteMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        deleteMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.deleteMessageAsync(deleteMessageRequest);
        }

        String receiptHandle = deleteMessageRequest.getReceiptHandle();
        String origReceiptHandle = receiptHandle;
        if (synchClient.isS3ReceiptHandle(receiptHandle)) {
            synchClient.deleteMessagePayloadFromS3(receiptHandle);
            origReceiptHandle = synchClient.getOrigReceiptHandle(receiptHandle);
        }
        deleteMessageRequest.setReceiptHandle(origReceiptHandle);
        return amazonSqsToBeExtended.deleteMessageAsync(deleteMessageRequest);
    }

    @Override
    public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest,
                                                          AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler) {

        if (deleteMessageRequest == null) {
            String errorMessage = "deleteMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        deleteMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.deleteMessageAsync(deleteMessageRequest, asyncHandler);
        }

        String receiptHandle = deleteMessageRequest.getReceiptHandle();
        String origReceiptHandle = receiptHandle;
        if (synchClient.isS3ReceiptHandle(receiptHandle)) {
            synchClient.deleteMessagePayloadFromS3(receiptHandle);
            origReceiptHandle = synchClient.getOrigReceiptHandle(receiptHandle);
        }
        deleteMessageRequest.setReceiptHandle(origReceiptHandle);
        return amazonSqsToBeExtended.deleteMessageAsync(deleteMessageRequest, asyncHandler);
    }

    @Override
    public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl, String receiptHandle) {
        return deleteMessageAsync(new DeleteMessageRequest(queueUrl, receiptHandle));
    }

    @Override
    public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl,
                                                          String receiptHandle,
                                                          AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler) {
        DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(queueUrl, receiptHandle);
        return deleteMessageAsync(deleteMessageRequest, asyncHandler);
    }

    @Override
    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(DeleteMessageBatchRequest deleteMessageBatchRequest) {

        if (deleteMessageBatchRequest == null) {
            String errorMessage = "deleteMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        deleteMessageBatchRequest.getRequestClientOptions().appendUserAgent(
                SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.deleteMessageBatchAsync(deleteMessageBatchRequest);
        }

        for (DeleteMessageBatchRequestEntry entry : deleteMessageBatchRequest.getEntries()) {
            String receiptHandle = entry.getReceiptHandle();
            String origReceiptHandle = receiptHandle;
            if (synchClient.isS3ReceiptHandle(receiptHandle)) {
                synchClient.deleteMessagePayloadFromS3(receiptHandle);
                origReceiptHandle = synchClient.getOrigReceiptHandle(receiptHandle);
            }
            entry.setReceiptHandle(origReceiptHandle);
        }
        return amazonSqsToBeExtended.deleteMessageBatchAsync(deleteMessageBatchRequest);
    }

    @Override
    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(DeleteMessageBatchRequest deleteMessageBatchRequest,
                                                                    AsyncHandler<DeleteMessageBatchRequest, DeleteMessageBatchResult> asyncHandler) {

        if (deleteMessageBatchRequest == null) {
            String errorMessage = "deleteMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        deleteMessageBatchRequest.getRequestClientOptions().appendUserAgent(
                SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.deleteMessageBatchAsync(deleteMessageBatchRequest, asyncHandler);
        }

        for (DeleteMessageBatchRequestEntry entry : deleteMessageBatchRequest.getEntries()) {
            String receiptHandle = entry.getReceiptHandle();
            String origReceiptHandle = receiptHandle;
            if (synchClient.isS3ReceiptHandle(receiptHandle)) {
                synchClient.deleteMessagePayloadFromS3(receiptHandle);
                origReceiptHandle = synchClient.getOrigReceiptHandle(receiptHandle);
            }
            entry.setReceiptHandle(origReceiptHandle);
        }
        return amazonSqsToBeExtended.deleteMessageBatchAsync(deleteMessageBatchRequest, asyncHandler);
    }

    @Override
    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(String queueUrl,
                                                                    List<DeleteMessageBatchRequestEntry> entries) {
        return deleteMessageBatchAsync(new DeleteMessageBatchRequest(queueUrl, entries));
    }

    @Override
    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(String queueUrl,
                                                                    List<DeleteMessageBatchRequestEntry> entries,
                                                                    AsyncHandler<DeleteMessageBatchRequest, DeleteMessageBatchResult> asyncHandler) {
        DeleteMessageBatchRequest deleteMessageBatchRequest = new DeleteMessageBatchRequest(queueUrl, entries);
        return deleteMessageBatchAsync(deleteMessageBatchRequest, asyncHandler);
    }

    @Override
    public Future<PurgeQueueResult> purgeQueueAsync(PurgeQueueRequest purgeQueueRequest) {
        LOG.warn("Calling purgeQueue deletes SQS messages without deleting their payload from S3.");

        if (purgeQueueRequest == null) {
            String errorMessage = "purgeQueueRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        purgeQueueRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        return amazonSqsToBeExtended.purgeQueueAsync(purgeQueueRequest);
    }

    @Override
    public Future<PurgeQueueResult> purgeQueueAsync(PurgeQueueRequest purgeQueueRequest,
                                                    AsyncHandler<PurgeQueueRequest, PurgeQueueResult> asyncHandler) {

        LOG.warn("Calling purgeQueue deletes SQS messages without deleting their payload from S3.");

        if (purgeQueueRequest == null) {
            String errorMessage = "purgeQueueRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        purgeQueueRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        return amazonSqsToBeExtended.purgeQueueAsync(purgeQueueRequest, asyncHandler);
    }

    @Override
    public Future<ReceiveMessageResult> receiveMessageAsync(ReceiveMessageRequest receiveMessageRequest) {

        if (receiveMessageRequest == null) {
            String errorMessage = "receiveMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        receiveMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.receiveMessageAsync(receiveMessageRequest);
        }

        if (!receiveMessageRequest.getMessageAttributeNames().contains(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME)) {
            receiveMessageRequest.getMessageAttributeNames().add(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
        }

        Future<ReceiveMessageResult> receiveMessageResultFuture = amazonSqsToBeExtended.receiveMessageAsync(receiveMessageRequest);
        ReceiveMessageResult receiveMessageResult;
        try {
            receiveMessageResult = receiveMessageResultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        List<Message> messages = receiveMessageResult.getMessages();
        extractMessagesStoredInS3(messages);
        return receiveMessageResultFuture;
    }

    private void extractMessagesStoredInS3(List<Message> messages) {
        for (Message message : messages) {

            // for each received message check if they are stored in S3.
            MessageAttributeValue largePayloadAttributeValue = message.getMessageAttributes().get(
                    SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
            if (largePayloadAttributeValue != null) {
                String messageBody = message.getBody();

                // read the S3 pointer from the message body JSON string.
                MessageS3Pointer s3Pointer = synchClient.readMessageS3PointerFromJSON(messageBody);

                String s3MsgBucketName = s3Pointer.getS3BucketName();
                String s3MsgKey = s3Pointer.getS3Key();

                String origMsgBody = synchClient.getTextFromS3(s3MsgBucketName, s3MsgKey);
                LOG.info("S3 object read, Bucket name: " + s3MsgBucketName + ", Object key: " + s3MsgKey + ".");

                message.setBody(origMsgBody);

                // remove the additional attribute before returning the message
                // to user.
                message.getMessageAttributes().remove(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);

                // Embed s3 object pointer in the receipt handle.
                String modifiedReceiptHandle = synchClient.embedS3PointerInReceiptHandle(message.getReceiptHandle(),
                                                                                         s3MsgBucketName, s3MsgKey);

                message.setReceiptHandle(modifiedReceiptHandle);
            }
        }
    }

    @Override
    public Future<ReceiveMessageResult> receiveMessageAsync(ReceiveMessageRequest receiveMessageRequest,
                                                            AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> asyncHandler) {

        if (receiveMessageRequest == null) {
            String errorMessage = "receiveMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        receiveMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.receiveMessageAsync(receiveMessageRequest, asyncHandler);
        }

        if (!receiveMessageRequest.getMessageAttributeNames().contains(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME)) {
            receiveMessageRequest.getMessageAttributeNames().add(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
        }

        Future<ReceiveMessageResult> receiveMessageResultFuture = amazonSqsToBeExtended.receiveMessageAsync(receiveMessageRequest, asyncHandler);
        ReceiveMessageResult receiveMessageResult;
        try {
            receiveMessageResult = receiveMessageResultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        List<Message> messages = receiveMessageResult.getMessages();
        extractMessagesStoredInS3(messages);
        return receiveMessageResultFuture;
    }

    @Override
    public Future<ReceiveMessageResult> receiveMessageAsync(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        return receiveMessageAsync(receiveMessageRequest);
    }

    @Override
    public Future<ReceiveMessageResult> receiveMessageAsync(String queueUrl,
                                                            AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> asyncHandler) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        return receiveMessageAsync(receiveMessageRequest, asyncHandler);
    }

    @Override
    public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest) {

        if (sendMessageRequest == null) {
            String errorMessage = "sendMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        sendMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.sendMessageAsync(sendMessageRequest);
        }

        if (sendMessageRequest.getMessageBody() == null || "".equals(sendMessageRequest.getMessageBody())) {
            String errorMessage = "messageBody cannot be null or empty.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        if (clientConfiguration.isAlwaysThroughS3() || synchClient.isLarge(sendMessageRequest)) {
            sendMessageRequest = synchClient.storeMessageInS3(sendMessageRequest);
        }
        return amazonSqsToBeExtended.sendMessageAsync(sendMessageRequest);
    }

    @Override
    public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest,
                                                      AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler) {

        if (sendMessageRequest == null) {
            String errorMessage = "sendMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        sendMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.sendMessageAsync(sendMessageRequest, asyncHandler);
        }

        if (sendMessageRequest.getMessageBody() == null || "".equals(sendMessageRequest.getMessageBody())) {
            String errorMessage = "messageBody cannot be null or empty.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        if (clientConfiguration.isAlwaysThroughS3() || synchClient.isLarge(sendMessageRequest)) {
            sendMessageRequest = synchClient.storeMessageInS3(sendMessageRequest);
        }
        return amazonSqsToBeExtended.sendMessageAsync(sendMessageRequest, asyncHandler);
    }

    @Override
    public Future<SendMessageResult> sendMessageAsync(String queueUrl, String messageBody) {
        return sendMessageAsync(new SendMessageRequest(queueUrl, messageBody));
    }

    @Override
    public Future<SendMessageResult> sendMessageAsync(String queueUrl,
                                                      String messageBody,
                                                      AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, messageBody);
        return sendMessageAsync(sendMessageRequest, asyncHandler);
    }

    @Override
    public Future<SendMessageBatchResult> sendMessageBatchAsync(SendMessageBatchRequest sendMessageBatchRequest) {

        if (sendMessageBatchRequest == null) {
            String errorMessage = "sendMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        sendMessageBatchRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.sendMessageBatchAsync(sendMessageBatchRequest);
        }

        List<SendMessageBatchRequestEntry> batchEntries = sendMessageBatchRequest.getEntries();

        int index = 0;
        for (SendMessageBatchRequestEntry entry : batchEntries) {
            if (clientConfiguration.isAlwaysThroughS3() || synchClient.isLarge(entry)) {
                batchEntries.set(index, synchClient.storeMessageInS3(entry));
            }
            ++index;
        }

        return amazonSqsToBeExtended.sendMessageBatchAsync(sendMessageBatchRequest);
    }

    @Override
    public Future<SendMessageBatchResult> sendMessageBatchAsync(SendMessageBatchRequest sendMessageBatchRequest,
                                                                AsyncHandler<SendMessageBatchRequest, SendMessageBatchResult> asyncHandler) {

        if (sendMessageBatchRequest == null) {
            String errorMessage = "sendMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        sendMessageBatchRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

        if (!clientConfiguration.isLargePayloadSupportEnabled()) {
            return amazonSqsToBeExtended.sendMessageBatchAsync(sendMessageBatchRequest, asyncHandler);
        }

        List<SendMessageBatchRequestEntry> batchEntries = sendMessageBatchRequest.getEntries();

        int index = 0;
        for (SendMessageBatchRequestEntry entry : batchEntries) {
            if (clientConfiguration.isAlwaysThroughS3() || synchClient.isLarge(entry)) {
                batchEntries.set(index, synchClient.storeMessageInS3(entry));
            }
            ++index;
        }

        return amazonSqsToBeExtended.sendMessageBatchAsync(sendMessageBatchRequest, asyncHandler);
    }

    @Override
    public Future<SendMessageBatchResult> sendMessageBatchAsync(String queueUrl,
                                                                List<SendMessageBatchRequestEntry> entries) {
        return sendMessageBatchAsync(new SendMessageBatchRequest(queueUrl, entries));
    }

    @Override
    public Future<SendMessageBatchResult> sendMessageBatchAsync(String queueUrl,
                                                                List<SendMessageBatchRequestEntry> entries,
                                                                AsyncHandler<SendMessageBatchRequest, SendMessageBatchResult> asyncHandler) {
        SendMessageBatchRequest sendMessageBatchRequest = new SendMessageBatchRequest(queueUrl, entries);
        return sendMessageBatchAsync(sendMessageBatchRequest, asyncHandler);
    }

    @Override
    public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
        return synchClient.sendMessage(sendMessageRequest);
    }

    @Override
    public SendMessageResult sendMessage(String queueUrl, String messageBody) {
        return synchClient.sendMessage(queueUrl, messageBody);
    }

    @Override
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        return synchClient.receiveMessage(receiveMessageRequest);
    }

    @Override
    public ReceiveMessageResult receiveMessage(String queueUrl) {
        return synchClient.receiveMessage(queueUrl);
    }

    @Override
    public DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        return synchClient.deleteMessage(deleteMessageRequest);
    }

    @Override
    public DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle) {
        return synchClient.deleteMessage(queueUrl, receiptHandle);
    }

    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl,
                                                                 String receiptHandle,
                                                                 Integer visibilityTimeout) {
        return synchClient.changeMessageVisibility(queueUrl, receiptHandle, visibilityTimeout);
    }

    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws AmazonClientException {

        return synchClient.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        return synchClient.sendMessageBatch(sendMessageBatchRequest);
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(String queueUrl, List<SendMessageBatchRequestEntry> entries) {
        return synchClient.sendMessageBatch(queueUrl, entries);
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {
        return synchClient.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries) {
        return synchClient.deleteMessageBatch(queueUrl, entries);
    }

    @Override
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(
            String queueUrl,
            java.util.List<ChangeMessageVisibilityBatchRequestEntry> entries) {
        return synchClient.changeMessageVisibilityBatch(queueUrl, entries);
    }

    @Override
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) throws AmazonClientException {
        return synchClient.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    @Override
    public PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest)
            throws AmazonClientException {
        return synchClient.purgeQueue(purgeQueueRequest);
    }
}
