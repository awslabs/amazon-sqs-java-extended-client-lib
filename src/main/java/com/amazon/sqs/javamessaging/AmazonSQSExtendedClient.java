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

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.BatchEntryIdsNotDistinctException;
import com.amazonaws.services.sqs.model.BatchRequestTooLongException;
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
import com.amazonaws.services.sqs.model.EmptyBatchRequestException;
import com.amazonaws.services.sqs.model.InvalidBatchEntryIdException;
import com.amazonaws.services.sqs.model.InvalidIdFormatException;
import com.amazonaws.services.sqs.model.InvalidMessageContentsException;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.MessageNotInflightException;
import com.amazonaws.services.sqs.model.OverLimitException;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.PurgeQueueResult;
import com.amazonaws.services.sqs.model.ReceiptHandleIsInvalidException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.sqs.model.TooManyEntriesInBatchRequestException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.payloadoffloading.*;


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
public class AmazonSQSExtendedClient extends AmazonSQSExtendedClientBase implements AmazonSQS {
    private static final Log LOG = LogFactory.getLog(AmazonSQSExtendedClient.class);
    private static final String USER_AGENT_HEADER = Util.getUserAgentHeader(AmazonSQSExtendedClient.class.getSimpleName());
    static final String LEGACY_RESERVED_ATTRIBUTE_NAME = "SQSLargePayloadSize";
    static final List<String> RESERVED_ATTRIBUTE_NAMES = Arrays.asList(LEGACY_RESERVED_ATTRIBUTE_NAME,
            SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
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
    public AmazonSQSExtendedClient(AmazonSQS sqsClient) {
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
    public AmazonSQSExtendedClient(AmazonSQS sqsClient, ExtendedClientConfiguration extendedClientConfig) {
        super(sqsClient);
        this.clientConfiguration = new ExtendedClientConfiguration(extendedClientConfig);
        S3Dao s3Dao = new S3Dao(clientConfiguration.getAmazonS3Client(),
                clientConfiguration.getSSEAwsKeyManagementParams(),
                clientConfiguration.getCannedAccessControlList());
        this.payloadStore = new S3BackedPayloadStore(s3Dao, clientConfiguration.getS3BucketName());
    }

    /**
     * <p>
     * Delivers a message to the specified queue and uploads the message payload
     * to Amazon S3 if necessary.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> The following list shows the characters (in Unicode)
     * allowed in your message, according to the W3C XML specification. For more
     * information, go to http://www.w3.org/TR/REC-xml/#charsets If you send any
     * characters not included in the list, your request will be rejected. #x9 |
     * #xA | #xD | [#x20 to #xD7FF] | [#xE000 to #xFFFD] | [#x10000 to #x10FFFF]
     * </p>
     *
     * <b>IMPORTANT:</b> The input object may be modified by the method. </p>
     *
     * @param sendMessageRequest
     *            Container for the necessary parameters to execute the
     *            SendMessage service method on AmazonSQS.
     *
     * @return The response from the SendMessage service method, as returned by
     *         AmazonSQS.
     *
     * @throws InvalidMessageContentsException
     * @throws UnsupportedOperationException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
        //TODO: Clone request since it's modified in this method and will cause issues if the client reuses request object.
        if (sendMessageRequest == null) {
            String errorMessage = "sendMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        sendMessageRequest.getRequestClientOptions().appendUserAgent(USER_AGENT_HEADER);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.sendMessage(sendMessageRequest);
        }

        if (sendMessageRequest.getMessageBody() == null || "".equals(sendMessageRequest.getMessageBody())) {
            String errorMessage = "messageBody cannot be null or empty.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        //Check message attributes for ExtendedClient related constraints
        checkMessageAttributes(sendMessageRequest.getMessageAttributes());

        if (clientConfiguration.isAlwaysThroughS3() || isLarge(sendMessageRequest)) {
            sendMessageRequest = storeMessageInS3(sendMessageRequest);
        }
        return super.sendMessage(sendMessageRequest);
    }

    /**
     * <p>
     * Delivers a message to the specified queue and uploads the message payload
     * to Amazon S3 if necessary.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> The following list shows the characters (in Unicode)
     * allowed in your message, according to the W3C XML specification. For more
     * information, go to http://www.w3.org/TR/REC-xml/#charsets If you send any
     * characters not included in the list, your request will be rejected. #x9 |
     * #xA | #xD | [#x20 to #xD7FF] | [#xE000 to #xFFFD] | [#x10000 to #x10FFFF]
     * </p>
     *
     * @param queueUrl
     *            The URL of the Amazon SQS queue to take action on.
     * @param messageBody
     *            The message to send. For a list of allowed characters, see the
     *            preceding important note.
     *
     * @return The response from the SendMessage service method, as returned by
     *         AmazonSQS.
     *
     * @throws InvalidMessageContentsException
     * @throws UnsupportedOperationException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public SendMessageResult sendMessage(String queueUrl, String messageBody) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, messageBody);
        return sendMessage(sendMessageRequest);
    }

    /**
     * <p>
     * Retrieves one or more messages, with a maximum limit of 10 messages, from
     * the specified queue. Downloads the message payloads from Amazon S3 when
     * necessary. Long poll support is enabled by using the
     * <code>WaitTimeSeconds</code> parameter. For more information, see <a
     * href=
     * "http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html"
     * > Amazon SQS Long Poll </a> in the <i>Amazon SQS Developer Guide</i> .
     * </p>
     * <p>
     * Short poll is the default behavior where a weighted random set of
     * machines is sampled on a <code>ReceiveMessage</code> call. This means
     * only the messages on the sampled machines are returned. If the number of
     * messages in the queue is small (less than 1000), it is likely you will
     * get fewer messages than you requested per <code>ReceiveMessage</code>
     * call. If the number of messages in the queue is extremely small, you
     * might not receive any messages in a particular
     * <code>ReceiveMessage</code> response; in which case you should repeat the
     * request.
     * </p>
     * <p>
     * For each message returned, the response includes the following:
     * </p>
     *
     * <ul>
     * <li>
     * <p>
     * Message body
     * </p>
     * </li>
     * <li>
     * <p>
     * MD5 digest of the message body. For information about MD5, go to <a
     * href="http://www.faqs.org/rfcs/rfc1321.html">
     * http://www.faqs.org/rfcs/rfc1321.html </a> .
     * </p>
     * </li>
     * <li>
     * <p>
     * Message ID you received when you sent the message to the queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * Receipt handle.
     * </p>
     * </li>
     * <li>
     * <p>
     * Message attributes.
     * </p>
     * </li>
     * <li>
     * <p>
     * MD5 digest of the message attributes.
     * </p>
     * </li>
     *
     * </ul>
     * <p>
     * The receipt handle is the identifier you must provide when deleting the
     * message. For more information, see <a href=
     * "http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/ImportantIdentifiers.html"
     * > Queue and Message Identifiers </a> in the <i>Amazon SQS Developer
     * Guide</i> .
     * </p>
     * <p>
     * You can provide the <code>VisibilityTimeout</code> parameter in your
     * request, which will be applied to the messages that Amazon SQS returns in
     * the response. If you do not include the parameter, the overall visibility
     * timeout for the queue is used for the returned messages. For more
     * information, see <a href=
     * "http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AboutVT.html"
     * > Visibility Timeout </a> in the <i>Amazon SQS Developer Guide</i> .
     * </p>
     * <p>
     * <b>NOTE:</b> Going forward, new attributes might be added. If you are
     * writing code that calls this action, we recommend that you structure your
     * code so that it can handle new attributes gracefully.
     * </p>
     *
     * @param receiveMessageRequest
     *            Container for the necessary parameters to execute the
     *            ReceiveMessage service method on AmazonSQS.
     *
     * @return The response from the ReceiveMessage service method, as returned
     *         by AmazonSQS.
     *
     * @throws OverLimitException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        //TODO: Clone request since it's modified in this method and will cause issues if the client reuses request object.
        if (receiveMessageRequest == null) {
            String errorMessage = "receiveMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        receiveMessageRequest.getRequestClientOptions().appendUserAgent(USER_AGENT_HEADER);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.receiveMessage(receiveMessageRequest);
        }
        //Remove before adding to avoid any duplicates
        receiveMessageRequest.getMessageAttributeNames().removeAll(RESERVED_ATTRIBUTE_NAMES);
        receiveMessageRequest.getMessageAttributeNames().addAll(RESERVED_ATTRIBUTE_NAMES);

        ReceiveMessageResult receiveMessageResult = super.receiveMessage(receiveMessageRequest);

        List<Message> messages = receiveMessageResult.getMessages();
        List<Message> messagesToIgnore = new ArrayList<>();
        for (Message message : messages) {

            // for each received message check if they are stored in S3.
            Optional<String> largePayloadAttributeName = getReservedAttributeNameIfPresent(message.getMessageAttributes());
            if (largePayloadAttributeName.isPresent()) {
                String largeMessagePointer = message.getBody();
                largeMessagePointer = largeMessagePointer.replace("com.amazon.sqs.javamessaging.MessageS3Pointer", "software.amazon.payloadoffloading.PayloadS3Pointer");

                final String originalBody;
                try {
                    originalBody = payloadStore.getOriginalPayload(largeMessagePointer);
                } catch (AmazonServiceException e) {
                    boolean isNoSuchKeyException = ((AmazonServiceException) e.getCause()).getErrorCode().equals("NoSuchKey");
                    if (isNoSuchKeyException && clientConfiguration.ignoresPayloadNotFound()) {
                        deleteMessage(receiveMessageRequest.getQueueUrl(), message.getReceiptHandle());
                        LOG.warn("SQS message deleted as it could not be found in S3");
                        messagesToIgnore.add(message);
                        continue;
                    }
                    throw e;
                }

                // Replace the large message pointer with the original message body
                message.setBody(originalBody);

                // remove the additional attribute before returning the message
                // to user.
                message.getMessageAttributes().keySet().removeAll(RESERVED_ATTRIBUTE_NAMES);

                // Embed s3 object pointer in the receipt handle.
                String modifiedReceiptHandle = embedS3PointerInReceiptHandle(
                        message.getReceiptHandle(),
                        largeMessagePointer);

                message.setReceiptHandle(modifiedReceiptHandle);
            }
        }
        receiveMessageResult.getMessages().removeAll(messagesToIgnore);
        return receiveMessageResult;
    }

    /**
     * <p>
     * Retrieves one or more messages, with a maximum limit of 10 messages, from
     * the specified queue. Downloads the message payloads from Amazon S3 when
     * necessary. Long poll support is enabled by using the
     * <code>WaitTimeSeconds</code> parameter. For more information, see <a
     * href=
     * "http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html"
     * > Amazon SQS Long Poll </a> in the <i>Amazon SQS Developer Guide</i> .
     * </p>
     * <p>
     * Short poll is the default behavior where a weighted random set of
     * machines is sampled on a <code>ReceiveMessage</code> call. This means
     * only the messages on the sampled machines are returned. If the number of
     * messages in the queue is small (less than 1000), it is likely you will
     * get fewer messages than you requested per <code>ReceiveMessage</code>
     * call. If the number of messages in the queue is extremely small, you
     * might not receive any messages in a particular
     * <code>ReceiveMessage</code> response; in which case you should repeat the
     * request.
     * </p>
     * <p>
     * For each message returned, the response includes the following:
     * </p>
     *
     * <ul>
     * <li>
     * <p>
     * Message body
     * </p>
     * </li>
     * <li>
     * <p>
     * MD5 digest of the message body. For information about MD5, go to <a
     * href="http://www.faqs.org/rfcs/rfc1321.html">
     * http://www.faqs.org/rfcs/rfc1321.html </a> .
     * </p>
     * </li>
     * <li>
     * <p>
     * Message ID you received when you sent the message to the queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * Receipt handle.
     * </p>
     * </li>
     * <li>
     * <p>
     * Message attributes.
     * </p>
     * </li>
     * <li>
     * <p>
     * MD5 digest of the message attributes.
     * </p>
     * </li>
     *
     * </ul>
     * <p>
     * The receipt handle is the identifier you must provide when deleting the
     * message. For more information, see <a href=
     * "http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/ImportantIdentifiers.html"
     * > Queue and Message Identifiers </a> in the <i>Amazon SQS Developer
     * Guide</i> .
     * </p>
     * <p>
     * You can provide the <code>VisibilityTimeout</code> parameter in your
     * request, which will be applied to the messages that Amazon SQS returns in
     * the response. If you do not include the parameter, the overall visibility
     * timeout for the queue is used for the returned messages. For more
     * information, see <a href=
     * "http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AboutVT.html"
     * > Visibility Timeout </a> in the <i>Amazon SQS Developer Guide</i> .
     * </p>
     * <p>
     * <b>NOTE:</b> Going forward, new attributes might be added. If you are
     * writing code that calls this action, we recommend that you structure your
     * code so that it can handle new attributes gracefully.
     * </p>
     *
     * @param queueUrl
     *            The URL of the Amazon SQS queue to take action on.
     *
     * @return The response from the ReceiveMessage service method, as returned
     *         by AmazonSQS.
     *
     * @throws OverLimitException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public ReceiveMessageResult receiveMessage(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        return receiveMessage(receiveMessageRequest);
    }

    /**
     * <p>
     * Deletes the specified message from the specified queue and deletes the
     * message payload from Amazon S3 when necessary. You specify the message by
     * using the message's <code>receipt handle</code> and not the
     * <code>message ID</code> you received when you sent the message. Even if
     * the message is locked by another reader due to the visibility timeout
     * setting, it is still deleted from the queue. If you leave a message in
     * the queue for longer than the queue's configured retention period, Amazon
     * SQS automatically deletes it.
     * </p>
     * <p>
     * <b>NOTE:</b> The receipt handle is associated with a specific instance of
     * receiving the message. If you receive a message more than once, the
     * receipt handle you get each time you receive the message is different.
     * When you request DeleteMessage, if you don't provide the most recently
     * received receipt handle for the message, the request will still succeed,
     * but the message might not be deleted.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> It is possible you will receive a message even after
     * you have deleted it. This might happen on rare occasions if one of the
     * servers storing a copy of the message is unavailable when you request to
     * delete the message. The copy remains on the server and might be returned
     * to you again on a subsequent receive request. You should create your
     * system to be idempotent so that receiving a particular message more than
     * once is not a problem.
     * </p>
     *
     * @param deleteMessageRequest
     *            Container for the necessary parameters to execute the
     *            DeleteMessage service method on AmazonSQS.
     *
     * @return The response from the DeleteMessage service method, as returned
     *         by AmazonSQS.
     *
     * @throws ReceiptHandleIsInvalidException
     * @throws InvalidIdFormatException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest) {

        if (deleteMessageRequest == null) {
            String errorMessage = "deleteMessageRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        deleteMessageRequest.getRequestClientOptions().appendUserAgent(USER_AGENT_HEADER);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.deleteMessage(deleteMessageRequest);
        }

        String receiptHandle = deleteMessageRequest.getReceiptHandle();
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

        deleteMessageRequest.setReceiptHandle(origReceiptHandle);
        return super.deleteMessage(deleteMessageRequest);
    }

    /**
     * <p>
     * Deletes the specified message from the specified queue and deletes the
     * message payload from Amazon S3 when necessary. You specify the message by
     * using the message's <code>receipt handle</code> and not the
     * <code>message ID</code> you received when you sent the message. Even if
     * the message is locked by another reader due to the visibility timeout
     * setting, it is still deleted from the queue. If you leave a message in
     * the queue for longer than the queue's configured retention period, Amazon
     * SQS automatically deletes it.
     * </p>
     * <p>
     * <b>NOTE:</b> The receipt handle is associated with a specific instance of
     * receiving the message. If you receive a message more than once, the
     * receipt handle you get each time you receive the message is different.
     * When you request DeleteMessage, if you don't provide the most recently
     * received receipt handle for the message, the request will still succeed,
     * but the message might not be deleted.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> It is possible you will receive a message even after
     * you have deleted it. This might happen on rare occasions if one of the
     * servers storing a copy of the message is unavailable when you request to
     * delete the message. The copy remains on the server and might be returned
     * to you again on a subsequent receive request. You should create your
     * system to be idempotent so that receiving a particular message more than
     * once is not a problem.
     * </p>
     *
     * @param queueUrl
     *            The URL of the Amazon SQS queue to take action on.
     * @param receiptHandle
     *            The receipt handle associated with the message to delete.
     *
     * @return The response from the DeleteMessage service method, as returned
     *         by AmazonSQS.
     *
     * @throws ReceiptHandleIsInvalidException
     * @throws InvalidIdFormatException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle) {
        DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(queueUrl, receiptHandle);
        return deleteMessage(deleteMessageRequest);
    }

    /**
     * Simplified method form for invoking the ChangeMessageVisibility
     * operation.
     *
     * @see #changeMessageVisibility(ChangeMessageVisibilityRequest)
     */
    public ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl,
                                                                 String receiptHandle,
                                                                 Integer visibilityTimeout) {
        ChangeMessageVisibilityRequest changeMessageVisibilityRequest =
                new ChangeMessageVisibilityRequest(queueUrl, receiptHandle, visibilityTimeout);
        return changeMessageVisibility(changeMessageVisibilityRequest);
    }

    /**
     * <p>
     * Changes the visibility timeout of a specified message in a queue to a new
     * value. The maximum allowed timeout value you can set the value to is 12
     * hours. This means you can't extend the timeout of a message in an
     * existing queue to more than a total visibility timeout of 12 hours. (For
     * more information visibility timeout, see <a href=
     * "http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AboutVT.html"
     * > Visibility Timeout </a> in the <i>Amazon SQS Developer Guide</i> .)
     * </p>
     * <p>
     * For example, let's say you have a message and its default message
     * visibility timeout is 30 minutes. You could call
     * <code>ChangeMessageVisiblity</code> with a value of two hours and the
     * effective timeout would be two hours and 30 minutes. When that time comes
     * near you could again extend the time out by calling
     * ChangeMessageVisiblity, but this time the maximum allowed timeout would
     * be 9 hours and 30 minutes.
     * </p>
     * <p>
     * <b>NOTE:</b> There is a 120,000 limit for the number of inflight messages
     * per queue. Messages are inflight after they have been received from the
     * queue by a consuming component, but have not yet been deleted from the
     * queue. If you reach the 120,000 limit, you will receive an OverLimit
     * error message from Amazon SQS. To help avoid reaching the limit, you
     * should delete the messages from the queue after they have been processed.
     * You can also increase the number of queues you use to process the
     * messages.
     * </p>
     * <p>
     * <b>IMPORTANT:</b>If you attempt to set the VisibilityTimeout to an amount
     * more than the maximum time left, Amazon SQS returns an error. It will not
     * automatically recalculate and increase the timeout to the maximum time
     * remaining.
     * </p>
     * <p>
     * <b>IMPORTANT:</b>Unlike with a queue, when you change the visibility
     * timeout for a specific message, that timeout value is applied immediately
     * but is not saved in memory for that message. If you don't delete a
     * message after it is received, the visibility timeout for the message the
     * next time it is received reverts to the original timeout value, not the
     * value you set with the ChangeMessageVisibility action.
     * </p>
     *
     * @param changeMessageVisibilityRequest
     *            Container for the necessary parameters to execute the
     *            ChangeMessageVisibility service method on AmazonSQS.
     *
     *
     * @throws ReceiptHandleIsInvalidException
     * @throws MessageNotInflightException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public ChangeMessageVisibilityResult changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws AmazonServiceException, AmazonClientException {

        if (isS3ReceiptHandle(changeMessageVisibilityRequest.getReceiptHandle())) {
            changeMessageVisibilityRequest.setReceiptHandle(
                    getOrigReceiptHandle(changeMessageVisibilityRequest.getReceiptHandle()));
        }
        return amazonSqsToBeExtended.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    /**
     * <p>
     * Delivers up to ten messages to the specified queue. This is a batch
     * version of SendMessage. The result of the send action on each message is
     * reported individually in the response. Uploads message payloads to Amazon
     * S3 when necessary.
     * </p>
     * <p>
     * If the <code>DelaySeconds</code> parameter is not specified for an entry,
     * the default for the queue is used.
     * </p>
     * <p>
     * <b>IMPORTANT:</b>The following list shows the characters (in Unicode)
     * that are allowed in your message, according to the W3C XML specification.
     * For more information, go to http://www.faqs.org/rfcs/rfc1321.html. If you
     * send any characters that are not included in the list, your request will
     * be rejected. #x9 | #xA | #xD | [#x20 to #xD7FF] | [#xE000 to #xFFFD] |
     * [#x10000 to #x10FFFF]
     * </p>
     * <p>
     * <b>IMPORTANT:</b> Because the batch request can result in a combination
     * of successful and unsuccessful actions, you should check for batch errors
     * even when the call returns an HTTP status code of 200.
     * </p>
     * <b>IMPORTANT:</b> The input object may be modified by the method. </p>
     * <p>
     * <b>NOTE:</b>Some API actions take lists of parameters. These lists are
     * specified using the param.n notation. Values of n are integers starting
     * from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&Attribute.1=this</code>
     * </p>
     * <p>
     * <code>&Attribute.2=that</code>
     * </p>
     *
     * @param sendMessageBatchRequest
     *            Container for the necessary parameters to execute the
     *            SendMessageBatch service method on AmazonSQS.
     *
     * @return The response from the SendMessageBatch service method, as
     *         returned by AmazonSQS.
     *
     * @throws BatchEntryIdsNotDistinctException
     * @throws TooManyEntriesInBatchRequestException
     * @throws BatchRequestTooLongException
     * @throws UnsupportedOperationException
     * @throws InvalidBatchEntryIdException
     * @throws EmptyBatchRequestException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {

        if (sendMessageBatchRequest == null) {
            String errorMessage = "sendMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        sendMessageBatchRequest.getRequestClientOptions().appendUserAgent(USER_AGENT_HEADER);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.sendMessageBatch(sendMessageBatchRequest);
        }

        List<SendMessageBatchRequestEntry> batchEntries = sendMessageBatchRequest.getEntries();

        int index = 0;
        for (SendMessageBatchRequestEntry entry : batchEntries) {
            //Check message attributes for ExtendedClient related constraints
            checkMessageAttributes(entry.getMessageAttributes());

            if (clientConfiguration.isAlwaysThroughS3() || isLarge(entry)) {
                batchEntries.set(index, storeMessageInS3(entry));
            }
            ++index;
        }

        return super.sendMessageBatch(sendMessageBatchRequest);
    }

    /**
     * <p>
     * Delivers up to ten messages to the specified queue. This is a batch
     * version of SendMessage. The result of the send action on each message is
     * reported individually in the response. Uploads message payloads to Amazon
     * S3 when necessary.
     * </p>
     * <p>
     * If the <code>DelaySeconds</code> parameter is not specified for an entry,
     * the default for the queue is used.
     * </p>
     * <p>
     * <b>IMPORTANT:</b>The following list shows the characters (in Unicode)
     * that are allowed in your message, according to the W3C XML specification.
     * For more information, go to http://www.faqs.org/rfcs/rfc1321.html. If you
     * send any characters that are not included in the list, your request will
     * be rejected. #x9 | #xA | #xD | [#x20 to #xD7FF] | [#xE000 to #xFFFD] |
     * [#x10000 to #x10FFFF]
     * </p>
     * <p>
     * <b>IMPORTANT:</b> Because the batch request can result in a combination
     * of successful and unsuccessful actions, you should check for batch errors
     * even when the call returns an HTTP status code of 200.
     * </p>
     * <p>
     * <b>NOTE:</b>Some API actions take lists of parameters. These lists are
     * specified using the param.n notation. Values of n are integers starting
     * from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&Attribute.1=this</code>
     * </p>
     * <p>
     * <code>&Attribute.2=that</code>
     * </p>
     *
     * @param queueUrl
     *            The URL of the Amazon SQS queue to take action on.
     * @param entries
     *            A list of <a>SendMessageBatchRequestEntry</a> items.
     *
     * @return The response from the SendMessageBatch service method, as
     *         returned by AmazonSQS.
     *
     * @throws BatchEntryIdsNotDistinctException
     * @throws TooManyEntriesInBatchRequestException
     * @throws BatchRequestTooLongException
     * @throws UnsupportedOperationException
     * @throws InvalidBatchEntryIdException
     * @throws EmptyBatchRequestException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public SendMessageBatchResult sendMessageBatch(String queueUrl, List<SendMessageBatchRequestEntry> entries) {
        SendMessageBatchRequest sendMessageBatchRequest = new SendMessageBatchRequest(queueUrl, entries);
        return sendMessageBatch(sendMessageBatchRequest);
    }

    /**
     * <p>
     * Deletes up to ten messages from the specified queue. This is a batch
     * version of DeleteMessage. The result of the delete action on each message
     * is reported individually in the response. Also deletes the message
     * payloads from Amazon S3 when necessary.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> Because the batch request can result in a combination
     * of successful and unsuccessful actions, you should check for batch errors
     * even when the call returns an HTTP status code of 200.
     * </p>
     * <p>
     * <b>NOTE:</b>Some API actions take lists of parameters. These lists are
     * specified using the param.n notation. Values of n are integers starting
     * from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&Attribute.1=this</code>
     * </p>
     * <p>
     * <code>&Attribute.2=that</code>
     * </p>
     *
     * @param deleteMessageBatchRequest
     *            Container for the necessary parameters to execute the
     *            DeleteMessageBatch service method on AmazonSQS.
     *
     * @return The response from the DeleteMessageBatch service method, as
     *         returned by AmazonSQS.
     *
     * @throws BatchEntryIdsNotDistinctException
     * @throws TooManyEntriesInBatchRequestException
     * @throws InvalidBatchEntryIdException
     * @throws EmptyBatchRequestException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {

        if (deleteMessageBatchRequest == null) {
            String errorMessage = "deleteMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        deleteMessageBatchRequest.getRequestClientOptions().appendUserAgent(USER_AGENT_HEADER);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.deleteMessageBatch(deleteMessageBatchRequest);
        }

        for (DeleteMessageBatchRequestEntry entry : deleteMessageBatchRequest.getEntries()) {
            String receiptHandle = entry.getReceiptHandle();
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

            entry.setReceiptHandle(origReceiptHandle);
        }
        return super.deleteMessageBatch(deleteMessageBatchRequest);
    }

    /**
     * <p>
     * Deletes up to ten messages from the specified queue. This is a batch
     * version of DeleteMessage. The result of the delete action on each message
     * is reported individually in the response. Also deletes the message
     * payloads from Amazon S3 when necessary.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> Because the batch request can result in a combination
     * of successful and unsuccessful actions, you should check for batch errors
     * even when the call returns an HTTP status code of 200.
     * </p>
     * <p>
     * <b>NOTE:</b>Some API actions take lists of parameters. These lists are
     * specified using the param.n notation. Values of n are integers starting
     * from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&Attribute.1=this</code>
     * </p>
     * <p>
     * <code>&Attribute.2=that</code>
     * </p>
     *
     * @param queueUrl
     *            The URL of the Amazon SQS queue to take action on.
     * @param entries
     *            A list of receipt handles for the messages to be deleted.
     *
     * @return The response from the DeleteMessageBatch service method, as
     *         returned by AmazonSQS.
     *
     * @throws BatchEntryIdsNotDistinctException
     * @throws TooManyEntriesInBatchRequestException
     * @throws InvalidBatchEntryIdException
     * @throws EmptyBatchRequestException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries) {
        DeleteMessageBatchRequest deleteMessageBatchRequest = new DeleteMessageBatchRequest(queueUrl, entries);
        return deleteMessageBatch(deleteMessageBatchRequest);
    }

    /**
     * Simplified method form for invoking the ChangeMessageVisibilityBatch
     * operation.
     *
     * @see #changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest)
     */
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(
            String queueUrl,
            java.util.List<ChangeMessageVisibilityBatchRequestEntry> entries) {
        ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest =
                new ChangeMessageVisibilityBatchRequest(queueUrl, entries);
        return changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    /**
     * <p>
     * Changes the visibility timeout of multiple messages. This is a batch
     * version of ChangeMessageVisibility. The result of the action on each
     * message is reported individually in the response. You can send up to 10
     * ChangeMessageVisibility requests with each
     * <code>ChangeMessageVisibilityBatch</code> action.
     * </p>
     * <p>
     * <b>IMPORTANT:</b>Because the batch request can result in a combination of
     * successful and unsuccessful actions, you should check for batch errors
     * even when the call returns an HTTP status code of 200.
     * </p>
     * <p>
     * <b>NOTE:</b>Some API actions take lists of parameters. These lists are
     * specified using the param.n notation. Values of n are integers starting
     * from 1. For example, a parameter list with two elements looks like this:
     * </p>
     * <p>
     * <code>&Attribute.1=this</code>
     * </p>
     * <p>
     * <code>&Attribute.2=that</code>
     * </p>
     *
     * @param changeMessageVisibilityBatchRequest
     *            Container for the necessary parameters to execute the
     *            ChangeMessageVisibilityBatch service method on AmazonSQS.
     *
     * @return The response from the ChangeMessageVisibilityBatch service
     *         method, as returned by AmazonSQS.
     *
     * @throws BatchEntryIdsNotDistinctException
     * @throws TooManyEntriesInBatchRequestException
     * @throws InvalidBatchEntryIdException
     * @throws EmptyBatchRequestException
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) throws AmazonServiceException,
            AmazonClientException {

        for (ChangeMessageVisibilityBatchRequestEntry entry : changeMessageVisibilityBatchRequest.getEntries()) {
            if (isS3ReceiptHandle(entry.getReceiptHandle())) {
                entry.setReceiptHandle(getOrigReceiptHandle(entry.getReceiptHandle()));
            }
        }

        return amazonSqsToBeExtended.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    /**
     * <p>
     * Deletes the messages in a queue specified by the <b>queue URL</b> .
     * </p>
     * <p>
     * <b>IMPORTANT:</b>When you use the PurgeQueue API, the deleted messages in
     * the queue cannot be retrieved.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> This does not delete the message payloads from Amazon S3.
     * </p>
     * <p>
     * When you purge a queue, the message deletion process takes up to 60
     * seconds. All messages sent to the queue before calling
     * <code>PurgeQueue</code> will be deleted; messages sent to the queue while
     * it is being purged may be deleted. While the queue is being purged,
     * messages sent to the queue before <code>PurgeQueue</code> was called may
     * be received, but will be deleted within the next minute.
     * </p>
     *
     * @param purgeQueueRequest
     *            Container for the necessary parameters to execute the
     *            PurgeQueue service method on AmazonSQS.
     * @return The response from the PurgeQueue service method, as returned
     *         by AmazonSQS.
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonSQS indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest)
            throws AmazonServiceException, AmazonClientException {
        LOG.warn("Calling purgeQueue deletes SQS messages without deleting their payload from S3.");

        if (purgeQueueRequest == null) {
            String errorMessage = "purgeQueueRequest cannot be null.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }

        purgeQueueRequest.getRequestClientOptions().appendUserAgent(USER_AGENT_HEADER);

        return super.purgeQueue(purgeQueueRequest);
    }

    private void checkMessageAttributes(Map<String, MessageAttributeValue> messageAttributes) {
        int msgAttributesSize = getMsgAttributesSize(messageAttributes);
        if (msgAttributesSize > clientConfiguration.getPayloadSizeThreshold()) {
            String errorMessage = "Total size of Message attributes is " + msgAttributesSize
                    + " bytes which is larger than the threshold of " + clientConfiguration.getPayloadSizeThreshold()
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
        Optional<String> largePayloadAttributeName = getReservedAttributeNameIfPresent(messageAttributes);

        if (largePayloadAttributeName.isPresent()) {
            String errorMessage = "Message attribute name " + largePayloadAttributeName.get()
                    + " is reserved for use by SQS extended client.";
            LOG.error(errorMessage);
            throw new AmazonClientException(errorMessage);
        }
    }

    /**
     * TODO: Wrap the message pointer as-is to the receiptHandle so that it can be generic
     * and does not use any LargeMessageStore implementation specific details.
     */
    private String embedS3PointerInReceiptHandle(String receiptHandle, String pointer) {
        PayloadS3Pointer s3Pointer = PayloadS3Pointer.fromJson(pointer);
        String s3MsgBucketName = s3Pointer.getS3BucketName();
        String s3MsgKey = s3Pointer.getS3Key();

        String modifiedReceiptHandle = SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + s3MsgBucketName
                + SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + SQSExtendedClientConstants.S3_KEY_MARKER
                + s3MsgKey + SQSExtendedClientConstants.S3_KEY_MARKER + receiptHandle;
        return modifiedReceiptHandle;
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

    private String getMessagePointerFromModifiedReceiptHandle(String receiptHandle) {
        String s3MsgBucketName = getFromReceiptHandleByMarker(receiptHandle, SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER);
        String s3MsgKey = getFromReceiptHandleByMarker(receiptHandle, SQSExtendedClientConstants.S3_KEY_MARKER);

        PayloadS3Pointer payloadS3Pointer = new PayloadS3Pointer(s3MsgBucketName, s3MsgKey);
        return payloadS3Pointer.toJson();
    }

    private boolean isLarge(SendMessageRequest sendMessageRequest) {
        int msgAttributesSize = getMsgAttributesSize(sendMessageRequest.getMessageAttributes());
        long msgBodySize = Util.getStringSizeInBytes(sendMessageRequest.getMessageBody());
        long totalMsgSize = msgAttributesSize + msgBodySize;
        return (totalMsgSize > clientConfiguration.getPayloadSizeThreshold());
    }

    private boolean isLarge(SendMessageBatchRequestEntry batchEntry) {
        int msgAttributesSize = getMsgAttributesSize(batchEntry.getMessageAttributes());
        long msgBodySize = Util.getStringSizeInBytes(batchEntry.getMessageBody());
        long totalMsgSize = msgAttributesSize + msgBodySize;
        return (totalMsgSize > clientConfiguration.getPayloadSizeThreshold());
    }

    private Optional<String> getReservedAttributeNameIfPresent(Map<String, MessageAttributeValue> msgAttributes) {
        String reservedAttributeName = null;
        if (msgAttributes.containsKey(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME)) {
            reservedAttributeName = SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME;
        } else if (msgAttributes.containsKey(LEGACY_RESERVED_ATTRIBUTE_NAME)) {
            reservedAttributeName = LEGACY_RESERVED_ATTRIBUTE_NAME;
        }
        return Optional.ofNullable(reservedAttributeName);
    }

    private int getMsgAttributesSize(Map<String, MessageAttributeValue> msgAttributes) {
        int totalMsgAttributesSize = 0;
        for (Entry<String, MessageAttributeValue> entry : msgAttributes.entrySet()) {
            totalMsgAttributesSize += Util.getStringSizeInBytes(entry.getKey());

            MessageAttributeValue entryVal = entry.getValue();
            if (entryVal.getDataType() != null) {
                totalMsgAttributesSize += Util.getStringSizeInBytes(entryVal.getDataType());
            }

            String stringVal = entryVal.getStringValue();
            if (stringVal != null) {
                totalMsgAttributesSize += Util.getStringSizeInBytes(entryVal.getStringValue());
            }

            ByteBuffer binaryVal = entryVal.getBinaryValue();
            if (binaryVal != null) {
                totalMsgAttributesSize += binaryVal.array().length;
            }
        }
        return totalMsgAttributesSize;
    }

    private SendMessageBatchRequestEntry storeMessageInS3(SendMessageBatchRequestEntry batchEntry) {

        // Read the content of the message from message body
        String messageContentStr = batchEntry.getMessageBody();

        Long messageContentSize = Util.getStringSizeInBytes(messageContentStr);

        // Add a new message attribute as a flag
        MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
        messageAttributeValue.setDataType("Number");
        messageAttributeValue.setStringValue(messageContentSize.toString());

        if (!clientConfiguration.usesLegacyReservedAttributeName()) {
            batchEntry.addMessageAttributesEntry(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME,
                    messageAttributeValue);
        } else {
            batchEntry.addMessageAttributesEntry(LEGACY_RESERVED_ATTRIBUTE_NAME,
                    messageAttributeValue);
        }

        // Store the message content in S3.
        String largeMessagePointer = payloadStore.storeOriginalPayload(messageContentStr,
                messageContentSize);
        batchEntry.setMessageBody(largeMessagePointer);

        return batchEntry;
    }

    private SendMessageRequest storeMessageInS3(SendMessageRequest sendMessageRequest) {

        // Read the content of the message from message body
        String messageContentStr = sendMessageRequest.getMessageBody();

        Long messageContentSize = Util.getStringSizeInBytes(messageContentStr);

        // Add a new message attribute as a flag
        MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
        messageAttributeValue.setDataType("Number");
        messageAttributeValue.setStringValue(messageContentSize.toString());

        if (!clientConfiguration.usesLegacyReservedAttributeName()) {
            sendMessageRequest.addMessageAttributesEntry(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME,
                    messageAttributeValue);
        } else {
            sendMessageRequest.addMessageAttributesEntry(LEGACY_RESERVED_ATTRIBUTE_NAME,
                    messageAttributeValue);
        }

        // Store the message content in S3.
        String largeMessagePointer = payloadStore.storeOriginalPayload(messageContentStr,
                messageContentSize);
        sendMessageRequest.setMessageBody(largeMessagePointer);

        return sendMessageRequest;
    }

}
