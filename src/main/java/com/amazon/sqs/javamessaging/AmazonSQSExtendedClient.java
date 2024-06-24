/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.checkMessageAttributes;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.embedS3PointerInReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.getMessagePointerFromModifiedReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.getOrigReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.getReservedAttributeNameIfPresent;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.isLarge;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.isS3ReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.sizeOf;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.updateMessageAttributePayloadSize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchEntryIdsNotDistinctException;
import software.amazon.awssdk.services.sqs.model.BatchRequestTooLongException;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.EmptyBatchRequestException;
import software.amazon.awssdk.services.sqs.model.InvalidBatchEntryIdException;
import software.amazon.awssdk.services.sqs.model.InvalidIdFormatException;
import software.amazon.awssdk.services.sqs.model.InvalidMessageContentsException;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageNotInflightException;
import software.amazon.awssdk.services.sqs.model.OverLimitException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueInProgressException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sqs.model.TooManyEntriesInBatchRequestException;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.payloadoffloading.PayloadStore;
import software.amazon.payloadoffloading.S3BackedPayloadStore;
import software.amazon.payloadoffloading.S3Dao;
import software.amazon.payloadoffloading.Util;


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
public class AmazonSQSExtendedClient extends AmazonSQSExtendedClientBase implements SqsClient {
    static final String USER_AGENT_NAME = AmazonSQSExtendedClient.class.getSimpleName();
    static final String USER_AGENT_VERSION = VersionInfo.SDK_VERSION;

    private static final Log LOG = LogFactory.getLog(AmazonSQSExtendedClient.class);
    private ExtendedClientConfiguration clientConfiguration;
    private PayloadStore payloadStore;

    /**
     * Constructs a new Amazon SQS extended client to invoke service methods on
     * Amazon SQS with extended functionality using the specified Amazon SQS
     * client object.
     *
     * <p>
     * All service calls made using this new client object are blocking, and
     * will not return until the service call completes.
     *
     * @param sqsClient
     *            The Amazon SQS client to use to connect to Amazon SQS.
     */
    public AmazonSQSExtendedClient(SqsClient sqsClient) {
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
     * @param sqsClient
     *            The Amazon SQS client to use to connect to Amazon SQS.
     * @param extendedClientConfig
     *            The extended client configuration options controlling the
     *            functionality of this client.
     */
    public AmazonSQSExtendedClient(SqsClient sqsClient, ExtendedClientConfiguration extendedClientConfig) {
        super(sqsClient);
        this.clientConfiguration = new ExtendedClientConfiguration(extendedClientConfig);
        S3Dao s3Dao = new S3Dao(clientConfiguration.getS3Client(),
                clientConfiguration.getServerSideEncryptionStrategy(),
                clientConfiguration.getObjectCannedACL());
        this.payloadStore = new S3BackedPayloadStore(s3Dao, clientConfiguration.getS3BucketName());
    }

    /**
     * <p>
     * Delivers a message to the specified queue.
     * </p>
     * <important>
     * <p>
     * A message can include only XML, JSON, and unformatted text. The following Unicode characters are allowed:
     * </p>
     * <p>
     * <code>#x9</code> | <code>#xA</code> | <code>#xD</code> | <code>#x20</code> to <code>#xD7FF</code> |
     * <code>#xE000</code> to <code>#xFFFD</code> | <code>#x10000</code> to <code>#x10FFFF</code>
     * </p>
     * <p>
     * Any characters not included in this list will be rejected. For more information, see the <a
     * href="http://www.w3.org/TR/REC-xml/#charsets">W3C specification for characters</a>.
     * </p>
     * </important>
     *
     * @param sendMessageRequest
     * @return Result of the SendMessage operation returned by the service.
     * @throws InvalidMessageContentsException
     *         The message contains characters outside the allowed set.
     * @throws UnsupportedOperationException
     *         Error code 400. Unsupported operation.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SendMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SendMessage" target="_top">AWS API
     *      Documentation</a>
     */
    public SendMessageResponse sendMessage(SendMessageRequest sendMessageRequest) {
        //TODO: Clone request since it's modified in this method and will cause issues if the client reuses request object.
        if (sendMessageRequest == null) {
            String errorMessage = "sendMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        SendMessageRequest.Builder sendMessageRequestBuilder = sendMessageRequest.toBuilder();
        sendMessageRequest = appendUserAgent(sendMessageRequestBuilder).build();

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.sendMessage(sendMessageRequest);
        }

        if (StringUtils.isEmpty(sendMessageRequest.messageBody())) {
            String errorMessage = "messageBody cannot be null or empty.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        //Check message attributes for ExtendedClient related constraints
        checkMessageAttributes(clientConfiguration.getPayloadSizeThreshold(), sendMessageRequest.messageAttributes());

        if (clientConfiguration.isAlwaysThroughS3()
            || isLarge(clientConfiguration.getPayloadSizeThreshold(), sendMessageRequest)) {
            sendMessageRequest = storeMessageInS3(sendMessageRequest);
        }
        return super.sendMessage(sendMessageRequest);
    }

    /**
     * <p>
     * Retrieves one or more messages (up to 10), from the specified queue. Using the <code>WaitTimeSeconds</code>
     * parameter enables long-poll support. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html">Amazon
     * SQS Long Polling</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * Short poll is the default behavior where a weighted random set of machines is sampled on a
     * <code>ReceiveMessage</code> call. Thus, only the messages on the sampled machines are returned. If the number of
     * messages in the queue is small (fewer than 1,000), you most likely get fewer messages than you requested per
     * <code>ReceiveMessage</code> call. If the number of messages in the queue is extremely small, you might not
     * receive any messages in a particular <code>ReceiveMessage</code> response. If this happens, repeat the request.
     * </p>
     * <p>
     * For each message returned, the response includes the following:
     * </p>
     * <ul>
     * <li>
     * <p>
     * The message body.
     * </p>
     * </li>
     * <li>
     * <p>
     * An MD5 digest of the message body. For information about MD5, see <a
     * href="https://www.ietf.org/rfc/rfc1321.txt">RFC1321</a>.
     * </p>
     * </li>
     * <li>
     * <p>
     * The <code>MessageId</code> you received when you sent the message to the queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * The receipt handle.
     * </p>
     * </li>
     * <li>
     * <p>
     * The message attributes.
     * </p>
     * </li>
     * <li>
     * <p>
     * An MD5 digest of the message attributes.
     * </p>
     * </li>
     * </ul>
     * <p>
     * The receipt handle is the identifier you must provide when deleting the message. For more information, see <a
     * href
     * ="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-message-identifiers.html"
     * >Queue and Message Identifiers</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * You can provide the <code>VisibilityTimeout</code> parameter in your request. The parameter is applied to the
     * messages that Amazon SQS returns in the response. If you don't include the parameter, the overall visibility
     * timeout for the queue is used for the returned messages. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html"
     * >Visibility Timeout</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * A message that isn't deleted or a message whose visibility isn't extended before the visibility timeout expires
     * counts as a failed receive. Depending on the configuration of the queue, the message might be sent to the
     * dead-letter queue.
     * </p>
     * <note>
     * <p>
     * In the future, new attributes might be added. If you write code that calls this action, we recommend that you
     * structure your code so that it can handle new attributes gracefully.
     * </p>
     * </note>
     *
     * @param receiveMessageRequest
     * @return Result of the ReceiveMessage operation returned by the service.
     * @throws OverLimitException
     *         The specified action violates a limit. For example, <code>ReceiveMessage</code> returns this error if the
     *         maximum number of inflight messages is reached and <code>AddPermission</code> returns this error if the
     *         maximum number of permissions for the queue is reached.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ReceiveMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ReceiveMessage" target="_top">AWS API
     *      Documentation</a>
     */
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        //TODO: Clone request since it's modified in this method and will cause issues if the client reuses request object.
        if (receiveMessageRequest == null) {
            String errorMessage = "receiveMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        ReceiveMessageRequest.Builder receiveMessageRequestBuilder = receiveMessageRequest.toBuilder();
        appendUserAgent(receiveMessageRequestBuilder);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.receiveMessage(receiveMessageRequestBuilder.build());
        }
        //Remove before adding to avoid any duplicates
        List<String> messageAttributeNames = new ArrayList<>(receiveMessageRequest.messageAttributeNames());
        messageAttributeNames.removeAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES);
        messageAttributeNames.addAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES);
        receiveMessageRequestBuilder.messageAttributeNames(messageAttributeNames);
        receiveMessageRequest = receiveMessageRequestBuilder.build();

        ReceiveMessageResponse receiveMessageResponse = super.receiveMessage(receiveMessageRequest);
        ReceiveMessageResponse.Builder receiveMessageResponseBuilder = receiveMessageResponse.toBuilder();

        List<Message> messages = receiveMessageResponse.messages();
        List<Message> modifiedMessages = new ArrayList<>(messages.size());
        for (Message message : messages) {
            Message.Builder messageBuilder = message.toBuilder();

            // for each received message check if they are stored in S3.
            Optional<String> largePayloadAttributeName = getReservedAttributeNameIfPresent(message.messageAttributes());
            if (largePayloadAttributeName.isPresent()) {
                String largeMessagePointer = message.body();
                largeMessagePointer = largeMessagePointer.replace("com.amazon.sqs.javamessaging.MessageS3Pointer", "software.amazon.payloadoffloading.PayloadS3Pointer");

                messageBuilder.body(payloadStore.getOriginalPayload(largeMessagePointer));

                // remove the additional attribute before returning the message
                // to user.
                Map<String, MessageAttributeValue> messageAttributes = new HashMap<>(message.messageAttributes());
                messageAttributes.keySet().removeAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES);
                messageBuilder.messageAttributes(messageAttributes);

                // Embed s3 object pointer in the receipt handle.
                String modifiedReceiptHandle = embedS3PointerInReceiptHandle(
                        message.receiptHandle(),
                        largeMessagePointer);

                messageBuilder.receiptHandle(modifiedReceiptHandle);
            }
            modifiedMessages.add(messageBuilder.build());
        }

        receiveMessageResponseBuilder.messages(modifiedMessages);
        return receiveMessageResponseBuilder.build();
    }

    /**
     * <p>
     * Deletes the specified message from the specified queue. To select the message to delete, use the
     * <code>ReceiptHandle</code> of the message (<i>not</i> the <code>MessageId</code> which you receive when you send
     * the message). Amazon SQS can delete a message from a queue even if a visibility timeout setting causes the
     * message to be locked by another consumer. Amazon SQS automatically deletes messages left in a queue longer than
     * the retention period configured for the queue.
     * </p>
     * <note>
     * <p>
     * The <code>ReceiptHandle</code> is associated with a <i>specific instance</i> of receiving a message. If you
     * receive a message more than once, the <code>ReceiptHandle</code> is different each time you receive a message.
     * When you use the <code>DeleteMessage</code> action, you must provide the most recently received
     * <code>ReceiptHandle</code> for the message (otherwise, the request succeeds, but the message might not be
     * deleted).
     * </p>
     * <p>
     * For standard queues, it is possible to receive a message even after you delete it. This might happen on rare
     * occasions if one of the servers which stores a copy of the message is unavailable when you send the request to
     * delete the message. The copy remains on the server and might be returned to you during a subsequent receive
     * request. You should ensure that your application is idempotent, so that receiving a message more than once does
     * not cause issues.
     * </p>
     * </note>
     *
     * @param deleteMessageRequest
     * @return Result of the DeleteMessage operation returned by the service.
     * @throws InvalidIdFormatException
     *         The specified receipt handle isn't valid for the current version.
     * @throws ReceiptHandleIsInvalidException
     *         The specified receipt handle isn't valid.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteMessage
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteMessage" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteMessageResponse deleteMessage(DeleteMessageRequest deleteMessageRequest) {

        if (deleteMessageRequest == null) {
            String errorMessage = "deleteMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        DeleteMessageRequest.Builder deleteMessageRequestBuilder = deleteMessageRequest.toBuilder();
        appendUserAgent(deleteMessageRequestBuilder);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.deleteMessage(deleteMessageRequestBuilder.build());
        }

        String receiptHandle = deleteMessageRequest.receiptHandle();
        String origReceiptHandle = receiptHandle;

        // Update original receipt handle if needed
        if (isS3ReceiptHandle(receiptHandle)) {
            origReceiptHandle = getOrigReceiptHandle(receiptHandle);
            // Delete pay load from S3 if needed
            if (clientConfiguration.doesCleanupS3Payload()) {
                String messagePointer = getMessagePointerFromModifiedReceiptHandle(receiptHandle);
                payloadStore.deleteOriginalPayload(messagePointer);
            }
        }

        deleteMessageRequestBuilder.receiptHandle(origReceiptHandle);
        return super.deleteMessage(deleteMessageRequestBuilder.build());
    }

    /**
     * <p>
     * Changes the visibility timeout of a specified message in a queue to a new value. The default visibility timeout
     * for a message is 30 seconds. The minimum is 0 seconds. The maximum is 12 hours. For more information, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html">
     * Visibility Timeout</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * <p>
     * For example, you have a message with a visibility timeout of 5 minutes. After 3 minutes, you call
     * <code>ChangeMessageVisibility</code> with a timeout of 10 minutes. You can continue to call
     * <code>ChangeMessageVisibility</code> to extend the visibility timeout to the maximum allowed time. If you try to
     * extend the visibility timeout beyond the maximum, your request is rejected.
     * </p>
     * <p>
     * An Amazon SQS message has three basic states:
     * </p>
     * <ol>
     * <li>
     * <p>
     * Sent to a queue by a producer.
     * </p>
     * </li>
     * <li>
     * <p>
     * Received from the queue by a consumer.
     * </p>
     * </li>
     * <li>
     * <p>
     * Deleted from the queue.
     * </p>
     * </li>
     * </ol>
     * <p>
     * A message is considered to be <i>stored</i> after it is sent to a queue by a producer, but not yet received from
     * the queue by a consumer (that is, between states 1 and 2). There is no limit to the number of stored messages. A
     * message is considered to be <i>in flight</i> after it is received from a queue by a consumer, but not yet deleted
     * from the queue (that is, between states 2 and 3). There is a limit to the number of inflight messages.
     * </p>
     * <p>
     * Limits that apply to inflight messages are unrelated to the <i>unlimited</i> number of stored messages.
     * </p>
     * <p>
     * For most standard queues (depending on queue traffic and message backlog), there can be a maximum of
     * approximately 120,000 inflight messages (received from a queue by a consumer, but not yet deleted from the
     * queue). If you reach this limit, Amazon SQS returns the <code>OverLimit</code> error message. To avoid reaching
     * the limit, you should delete messages from the queue after they're processed. You can also increase the number of
     * queues you use to process your messages. To request a limit increase, <a href=
     * "https://console.aws.amazon.com/support/home#/case/create?issueType=service-limit-increase&amp;limitType=service-code-sqs"
     * >file a support request</a>.
     * </p>
     * <p>
     * For FIFO queues, there can be a maximum of 20,000 inflight messages (received from a queue by a consumer, but not
     * yet deleted from the queue). If you reach this limit, Amazon SQS returns no error messages.
     * </p>
     * <important>
     * <p>
     * If you attempt to set the <code>VisibilityTimeout</code> to a value greater than the maximum time left, Amazon
     * SQS returns an error. Amazon SQS doesn't automatically recalculate and increase the timeout to the maximum
     * remaining time.
     * </p>
     * <p>
     * Unlike with a queue, when you change the visibility timeout for a specific message the timeout value is applied
     * immediately but isn't saved in memory for that message. If you don't delete a message after it is received, the
     * visibility timeout for the message reverts to the original timeout value (not to the value you set using the
     * <code>ChangeMessageVisibility</code> action) the next time the message is received.
     * </p>
     * </important>
     *
     * @param changeMessageVisibilityRequest
     * @return Result of the ChangeMessageVisibility operation returned by the service.
     * @throws MessageNotInflightException
     *         The specified message isn't in flight.
     * @throws ReceiptHandleIsInvalidException
     *         The specified receipt handle isn't valid.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ChangeMessageVisibility
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ChangeMessageVisibility" target="_top">AWS
     *      API Documentation</a>
     */
    public ChangeMessageVisibilityResponse changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws AwsServiceException, SdkClientException {

        ChangeMessageVisibilityRequest.Builder changeMessageVisibilityRequestBuilder = changeMessageVisibilityRequest.toBuilder();
        if (isS3ReceiptHandle(changeMessageVisibilityRequest.receiptHandle())) {
            changeMessageVisibilityRequestBuilder.receiptHandle(
                    getOrigReceiptHandle(changeMessageVisibilityRequest.receiptHandle()));
        }
        return amazonSqsToBeExtended.changeMessageVisibility(changeMessageVisibilityRequestBuilder.build());
    }

    /**
     * <p>
     * Delivers up to ten messages to the specified queue. This is a batch version of <code> <a>SendMessage</a>.</code>
     * For a FIFO queue, multiple messages within a single batch are enqueued in the order they are sent.
     * </p>
     * <p>
     * The result of sending each message is reported individually in the response. Because the batch request can result
     * in a combination of successful and unsuccessful actions, you should check for batch errors even when the call
     * returns an HTTP status code of <code>200</code>.
     * </p>
     * <p>
     * The maximum allowed individual message size and the maximum total payload size (the sum of the individual lengths
     * of all of the batched messages) are both 256 KB (262,144 bytes).
     * </p>
     * <important>
     * <p>
     * A message can include only XML, JSON, and unformatted text. The following Unicode characters are allowed:
     * </p>
     * <p>
     * <code>#x9</code> | <code>#xA</code> | <code>#xD</code> | <code>#x20</code> to <code>#xD7FF</code> |
     * <code>#xE000</code> to <code>#xFFFD</code> | <code>#x10000</code> to <code>#x10FFFF</code>
     * </p>
     * <p>
     * Any characters not included in this list will be rejected. For more information, see the <a
     * href="http://www.w3.org/TR/REC-xml/#charsets">W3C specification for characters</a>.
     * </p>
     * </important>
     * <p>
     * If you don't specify the <code>DelaySeconds</code> parameter for an entry, Amazon SQS uses the default value for
     * the queue.
     * </p>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;AttributeName.1=first</code>
     * </p>
     * <p>
     * <code>&amp;AttributeName.2=second</code>
     * </p>
     *
     * @param sendMessageBatchRequest
     * @return Result of the SendMessageBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws BatchRequestTooLongException
     *         The length of all the messages put together is more than the limit.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws UnsupportedOperationException
     *         Error code 400. Unsupported operation.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SendMessageBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SendMessageBatch" target="_top">AWS API
     *      Documentation</a>
     */
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {

        if (sendMessageBatchRequest == null) {
            String errorMessage = "sendMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        SendMessageBatchRequest.Builder sendMessageBatchRequestBuilder = sendMessageBatchRequest.toBuilder();
        appendUserAgent(sendMessageBatchRequestBuilder);
        sendMessageBatchRequest = sendMessageBatchRequestBuilder.build();

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.sendMessageBatch(sendMessageBatchRequest);
        }

        List<SendMessageBatchRequestEntry> originalEntries = sendMessageBatchRequest.entries();
        ArrayList<SendMessageBatchRequestEntry> alteredEntries = new ArrayList<>(originalEntries.size());
        alteredEntries.addAll(originalEntries);

        // Batch entry sizes order by size
        List<Pair<Integer, Long>> entrySizes = IntStream.range(0, originalEntries.size())
                .boxed()
                .map(i -> Pair.of(i, sizeOf(originalEntries.get(i))))
                .sorted((p1, p2) -> Long.compare(p2.right(), p1.right()))
                .collect(Collectors.toList());

        long totalSize = entrySizes.stream().map(Pair::right).mapToLong(Long::longValue).sum();

        // Move messages to s3 starting from the largest until total size is under the threshold if needed
        boolean hasS3Entries = false;
        for (Pair<Integer, Long> pair : entrySizes) {
            // Verify that total size of batch request is within limits
            if (totalSize <= clientConfiguration.getPayloadSizeThreshold() && !clientConfiguration.isAlwaysThroughS3()) {
                break;
            }
            Integer entryIndex = pair.left();
            Long originalEntrySize = pair.right();
            SendMessageBatchRequestEntry originalEntry = originalEntries.get(entryIndex);
            checkMessageAttributes(clientConfiguration.getPayloadSizeThreshold(), originalEntry.messageAttributes());
            SendMessageBatchRequestEntry alteredEntry = storeMessageInS3(originalEntry);
            totalSize = totalSize - originalEntrySize + sizeOf(alteredEntry);
            alteredEntries.set(entryIndex, alteredEntry);
            hasS3Entries = true;
        }

        if (hasS3Entries) {
            sendMessageBatchRequest = sendMessageBatchRequest.toBuilder().entries(alteredEntries).build();
        }

        return super.sendMessageBatch(sendMessageBatchRequest);
    }

    /**
     * <p>
     * Deletes up to ten messages from the specified queue. This is a batch version of
     * <code> <a>DeleteMessage</a>.</code> The result of the action on each message is reported individually in the
     * response.
     * </p>
     * <important>
     * <p>
     * Because the batch request can result in a combination of successful and unsuccessful actions, you should check
     * for batch errors even when the call returns an HTTP status code of <code>200</code>.
     * </p>
     * </important>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;AttributeName.1=first</code>
     * </p>
     * <p>
     * <code>&amp;AttributeName.2=second</code>
     * </p>
     *
     * @param deleteMessageBatchRequest
     * @return Result of the DeleteMessageBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteMessageBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteMessageBatch" target="_top">AWS API
     *      Documentation</a>
     */
    public DeleteMessageBatchResponse deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {

        if (deleteMessageBatchRequest == null) {
            String errorMessage = "deleteMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        DeleteMessageBatchRequest.Builder deleteMessageBatchRequestBuilder = deleteMessageBatchRequest.toBuilder();
        appendUserAgent(deleteMessageBatchRequestBuilder);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.deleteMessageBatch(deleteMessageBatchRequest);
        }

        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>(deleteMessageBatchRequest.entries().size());
        for (DeleteMessageBatchRequestEntry entry : deleteMessageBatchRequest.entries()) {
            DeleteMessageBatchRequestEntry.Builder entryBuilder = entry.toBuilder();
            String receiptHandle = entry.receiptHandle();
            String origReceiptHandle = receiptHandle;

            // Update original receipt handle if needed
            if (isS3ReceiptHandle(receiptHandle)) {
                origReceiptHandle = getOrigReceiptHandle(receiptHandle);
                // Delete s3 payload if needed
                if (clientConfiguration.doesCleanupS3Payload()) {
                    String messagePointer = getMessagePointerFromModifiedReceiptHandle(receiptHandle);
                    payloadStore.deleteOriginalPayload(messagePointer);
                }
            }

            entryBuilder.receiptHandle(origReceiptHandle);
            entries.add(entryBuilder.build());
        }

        deleteMessageBatchRequestBuilder.entries(entries);
        return super.deleteMessageBatch(deleteMessageBatchRequestBuilder.build());
    }

    /**
     * <p>
     * Changes the visibility timeout of multiple messages. This is a batch version of
     * <code> <a>ChangeMessageVisibility</a>.</code> The result of the action on each message is reported individually
     * in the response. You can send up to 10 <code> <a>ChangeMessageVisibility</a> </code> requests with each
     * <code>ChangeMessageVisibilityBatch</code> action.
     * </p>
     * <important>
     * <p>
     * Because the batch request can result in a combination of successful and unsuccessful actions, you should check
     * for batch errors even when the call returns an HTTP status code of <code>200</code>.
     * </p>
     * </important>
     * <p>
     * Some actions take lists of parameters. These lists are specified using the <code>param.n</code> notation. Values
     * of <code>n</code> are integers starting from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&amp;AttributeName.1=first</code>
     * </p>
     * <p>
     * <code>&amp;AttributeName.2=second</code>
     * </p>
     *
     * @param changeMessageVisibilityBatchRequest
     * @return Result of the ChangeMessageVisibilityBatch operation returned by the service.
     * @throws TooManyEntriesInBatchRequestException
     *         The batch request contains more entries than permissible.
     * @throws EmptyBatchRequestException
     *         The batch request doesn't contain any entries.
     * @throws BatchEntryIdsNotDistinctException
     *         Two or more batch entries in the request have the same <code>Id</code>.
     * @throws InvalidBatchEntryIdException
     *         The <code>Id</code> of a batch entry in a batch request doesn't abide by the specification.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ChangeMessageVisibilityBatch
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ChangeMessageVisibilityBatch"
     *      target="_top">AWS API Documentation</a>
     */
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) throws AwsServiceException,
            SdkClientException {

        List<ChangeMessageVisibilityBatchRequestEntry> entries = new ArrayList<>(changeMessageVisibilityBatchRequest.entries().size());
        for (ChangeMessageVisibilityBatchRequestEntry entry : changeMessageVisibilityBatchRequest.entries()) {
            ChangeMessageVisibilityBatchRequestEntry.Builder entryBuilder = entry.toBuilder();
            if (isS3ReceiptHandle(entry.receiptHandle())) {
                entryBuilder.receiptHandle(getOrigReceiptHandle(entry.receiptHandle()));
            }
            entries.add(entryBuilder.build());
        }

        return amazonSqsToBeExtended.changeMessageVisibilityBatch(
            changeMessageVisibilityBatchRequest.toBuilder().entries(entries).build());
    }

    /**
     * <p>
     * Deletes the messages in a queue specified by the <code>QueueURL</code> parameter.
     * </p>
     * <important>
     * <p>
     * When you use the <code>PurgeQueue</code> action, you can't retrieve any messages deleted from a queue.
     * </p>
     * <p>
     * The message deletion process takes up to 60 seconds. We recommend waiting for 60 seconds regardless of your
     * queue's size.
     * </p>
     * </important>
     * <p>
     * Messages sent to the queue <i>before</i> you call <code>PurgeQueue</code> might be received but are deleted
     * within the next minute.
     * </p>
     * <p>
     * Messages sent to the queue <i>after</i> you call <code>PurgeQueue</code> might be deleted while the queue is
     * being purged.
     * </p>
     *
     * @param purgeQueueRequest
     * @return Result of the PurgeQueue operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws PurgeQueueInProgressException
     *         Indicates that the specified queue previously received a <code>PurgeQueue</code> request within the last
     *         60 seconds (the time it can take to delete the messages in the queue).
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.PurgeQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/PurgeQueue" target="_top">AWS API
     *      Documentation</a>
     */
    public PurgeQueueResponse purgeQueue(PurgeQueueRequest purgeQueueRequest)
            throws AwsServiceException, SdkClientException {
        LOG.warn("Calling purgeQueue deletes SQS messages without deleting their payload from S3.");

        if (purgeQueueRequest == null) {
            String errorMessage = "purgeQueueRequest cannot be null.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        PurgeQueueRequest.Builder purgeQueueRequestBuilder = purgeQueueRequest.toBuilder();
        appendUserAgent(purgeQueueRequestBuilder);

        return super.purgeQueue(purgeQueueRequestBuilder.build());
    }

    private SendMessageBatchRequestEntry storeMessageInS3(SendMessageBatchRequestEntry batchEntry) {

        // Read the content of the message from message body
        String messageContentStr = batchEntry.messageBody();

        Long messageContentSize = Util.getStringSizeInBytes(messageContentStr);

        SendMessageBatchRequestEntry.Builder batchEntryBuilder = batchEntry.toBuilder();

        batchEntryBuilder.messageAttributes(
            updateMessageAttributePayloadSize(batchEntry.messageAttributes(), messageContentSize,
                clientConfiguration.usesLegacyReservedAttributeName()));

        // Store the message content in S3.
        String largeMessagePointer = storeOriginalPayload(messageContentStr);
        batchEntryBuilder.messageBody(largeMessagePointer);

        return batchEntryBuilder.build();
    }

    private SendMessageRequest storeMessageInS3(SendMessageRequest sendMessageRequest) {

        // Read the content of the message from message body
        String messageContentStr = sendMessageRequest.messageBody();

        Long messageContentSize = Util.getStringSizeInBytes(messageContentStr);

        SendMessageRequest.Builder sendMessageRequestBuilder = sendMessageRequest.toBuilder();

        sendMessageRequestBuilder.messageAttributes(
            updateMessageAttributePayloadSize(sendMessageRequest.messageAttributes(), messageContentSize,
                clientConfiguration.usesLegacyReservedAttributeName()));

        // Store the message content in S3.
        String largeMessagePointer = storeOriginalPayload(messageContentStr);
        sendMessageRequestBuilder.messageBody(largeMessagePointer);

        return sendMessageRequestBuilder.build();
    }

    private String storeOriginalPayload(String messageContentStr) {
        String s3KeyPrefix = clientConfiguration.getS3KeyPrefix();
        if (StringUtils.isBlank(s3KeyPrefix)) {
            return payloadStore.storeOriginalPayload(messageContentStr);
        }
        return payloadStore.storeOriginalPayload(messageContentStr, s3KeyPrefix + UUID.randomUUID());
    }

    @SuppressWarnings("unchecked")
    private static <T extends AwsRequest.Builder> T appendUserAgent(final T builder) {
        return AmazonSQSExtendedClientUtil.appendUserAgent(builder, USER_AGENT_NAME, USER_AGENT_VERSION);
    }

	@Override
	public void close() {
		super.close();
		this.clientConfiguration.getS3Client().close();
	}

}
