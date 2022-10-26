package com.amazon.sqs.javamessaging;

import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.checkMessageAttributes;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.embedS3PointerInReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.getMessagePointerFromModifiedReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.getOrigReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.getReservedAttributeNameIfPresent;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.isLarge;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.isS3ReceiptHandle;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClientUtil.updateMessageAttributePayloadSize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
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
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.payloadoffloading.PayloadStoreAsync;
import software.amazon.payloadoffloading.S3AsyncDao;
import software.amazon.payloadoffloading.S3BackedPayloadStoreAsync;
import software.amazon.payloadoffloading.Util;

/**
 * Amazon SQS Extended Async Client extends the functionality of Amazon Async SQS
 * client. All service calls made using this client are asynchronous, and will return
 * immediately with a {@link CompletableFuture} that completes when the operation
 * completes or when an exception is thrown.
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
public class AmazonSQSExtendedAsyncClient extends AmazonSQSExtendedAsyncClientBase implements SqsAsyncClient {
    static final String USER_AGENT_NAME = AmazonSQSExtendedAsyncClient.class.getSimpleName();
    static final String USER_AGENT_VERSION = VersionInfo.SDK_VERSION;

    private static final Log LOG = LogFactory.getLog(AmazonSQSExtendedAsyncClient.class);
    private ExtendedAsyncClientConfiguration clientConfiguration;
    private PayloadStoreAsync payloadStore;

    /**
     * Constructs a new Amazon SQS extended async client to invoke service methods on
     * Amazon SQS with extended functionality using the specified Amazon SQS
     * client object.
     *
     * <p>
     * All service calls made using this client are asynchronous, and will return
     * immediately with a {@link CompletableFuture} that completes when the operation
     * completes or when an exception is thrown.
     *
     * @param sqsClient
     *            The Amazon SQS async client to use to connect to Amazon SQS.
     */
    public AmazonSQSExtendedAsyncClient(SqsAsyncClient sqsClient) {
        this(sqsClient, new ExtendedAsyncClientConfiguration());
    }

    /**
     * Constructs a new Amazon SQS extended client to invoke service methods on
     * Amazon SQS with extended functionality using the specified Amazon SQS
     * client object.
     *
     * <p>
     * All service calls made using this client are asynchronous, and will return
     * immediately with a {@link CompletableFuture} that completes when the operation
     * completes or when an exception is thrown.
     *
     * @param sqsClient
     *            The Amazon SQS async client to use to connect to Amazon SQS.
     * @param extendedClientConfig
     *            The extended client configuration options controlling the
     *            functionality of this client.
     */
    public AmazonSQSExtendedAsyncClient(SqsAsyncClient sqsClient,
                                        ExtendedAsyncClientConfiguration extendedClientConfig) {
        super(sqsClient);
        this.clientConfiguration = new ExtendedAsyncClientConfiguration(extendedClientConfig);
        S3AsyncDao s3Dao = new S3AsyncDao(clientConfiguration.getS3AsyncClient(),
            clientConfiguration.getServerSideEncryptionStrategy(),
            clientConfiguration.getObjectCannedACL());
        this.payloadStore = new S3BackedPayloadStoreAsync(s3Dao, clientConfiguration.getS3BucketName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        // TODO: Clone request since it's modified in this method and will cause issues if the client reuses request
        // object.
        if (sendMessageRequest == null) {
            String errorMessage = "sendMessageRequest cannot be null.";
            LOG.error(errorMessage);
            CompletableFuture<SendMessageResponse> futureEx = new CompletableFuture<>();
            futureEx.completeExceptionally(SdkClientException.create(errorMessage));
            return futureEx;
        }

        SendMessageRequest.Builder sendMessageRequestBuilder = sendMessageRequest.toBuilder();
        sendMessageRequest = appendUserAgent(sendMessageRequestBuilder).build();

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.sendMessage(sendMessageRequest);
        }

        if (StringUtils.isEmpty(sendMessageRequest.messageBody())) {
            String errorMessage = "messageBody cannot be null or empty.";
            LOG.error(errorMessage);
            CompletableFuture<SendMessageResponse> futureEx = new CompletableFuture<>();
            futureEx.completeExceptionally(SdkClientException.create(errorMessage));
            return futureEx;
        }

        //Check message attributes for ExtendedClient related constraints
        try {
            checkMessageAttributes(clientConfiguration.getPayloadSizeThreshold(), sendMessageRequest.messageAttributes());
        } catch (SdkClientException e) {
            CompletableFuture<SendMessageResponse> futureEx = new CompletableFuture<>();
            futureEx.completeExceptionally(e);
            return futureEx;
        }

        if (clientConfiguration.isAlwaysThroughS3()
            || isLarge(clientConfiguration.getPayloadSizeThreshold(), sendMessageRequest)) {
            return storeMessageInS3(sendMessageRequest)
                .thenCompose(modifiedRequest -> super.sendMessage(modifiedRequest));
        }

        return super.sendMessage(sendMessageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        // TODO: Clone request since it's modified in this method and will cause issues if the client reuses request
        // object.
        if (receiveMessageRequest == null) {
            String errorMessage = "receiveMessageRequest cannot be null.";
            LOG.error(errorMessage);

            CompletableFuture<ReceiveMessageResponse> future = new CompletableFuture<>();
            future.completeExceptionally(SdkClientException.create(errorMessage));
            return future;
        }

        ReceiveMessageRequest.Builder receiveMessageRequestBuilder = receiveMessageRequest.toBuilder();
        appendUserAgent(receiveMessageRequestBuilder);

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.receiveMessage(receiveMessageRequestBuilder.build());
        }

        // Remove before adding to avoid any duplicates
        List<String> messageAttributeNames = new ArrayList<>(receiveMessageRequest.messageAttributeNames());
        messageAttributeNames.removeAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES);
        messageAttributeNames.addAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES);
        receiveMessageRequestBuilder.messageAttributeNames(messageAttributeNames);
        receiveMessageRequest = receiveMessageRequestBuilder.build();

        return super.receiveMessage(receiveMessageRequest)
            .thenCompose(receiveMessageResponse -> {
                ReceiveMessageResponse.Builder receiveMessageResponseBuilder = receiveMessageResponse.toBuilder();

                List<Message> messages = receiveMessageResponse.messages();
                List<CompletableFuture<Message>> modifiedMessageFutures = new ArrayList<>(messages.size());
                for (Message message : messages) {
                    Message.Builder messageBuilder = message.toBuilder();

                    // For each received message check if they are stored in S3.
                    Optional<String> largePayloadAttributeName = getReservedAttributeNameIfPresent(
                        message.messageAttributes());
                    if (!largePayloadAttributeName.isPresent()) {
                        // Not S3
                        modifiedMessageFutures.add(CompletableFuture.completedFuture(messageBuilder.build()));
                    } else {
                        // In S3
                        final String largeMessagePointer = message.body()
                            .replace("com.amazon.sqs.javamessaging.MessageS3Pointer",
                                "software.amazon.payloadoffloading.PayloadS3Pointer");

                        // Retrieve original payload
                        modifiedMessageFutures.add(payloadStore.getOriginalPayload(largeMessagePointer)
                            .thenApply(originalPayload -> {
                                // Set original payload
                                messageBuilder.body(originalPayload);

                                // Remove the additional attribute before returning the message
                                // to user.
                                Map<String, MessageAttributeValue> messageAttributes = new HashMap<>(
                                    message.messageAttributes());
                                messageAttributes.keySet().removeAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES);
                                messageBuilder.messageAttributes(messageAttributes);

                                // Embed s3 object pointer in the receipt handle.
                                String modifiedReceiptHandle = embedS3PointerInReceiptHandle(
                                    message.receiptHandle(),
                                    largeMessagePointer);
                                messageBuilder.receiptHandle(modifiedReceiptHandle);

                                return messageBuilder.build();
                            }));
                    }
                }

                // Convert list of message futures to a future list of messages.
                return CompletableFuture.allOf(
                        modifiedMessageFutures.toArray(new CompletableFuture[modifiedMessageFutures.size()]))
                    .thenApply(v -> modifiedMessageFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
            })
            .thenApply(modifiedMessages -> {
                // Build response with modified message list.
                ReceiveMessageResponse.Builder receiveMessageResponseBuilder = ReceiveMessageResponse.builder();
                receiveMessageResponseBuilder.messages(modifiedMessages);
                return receiveMessageResponseBuilder.build();
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        if (deleteMessageRequest == null) {
            String errorMessage = "deleteMessageRequest cannot be null.";
            LOG.error(errorMessage);

            CompletableFuture<DeleteMessageResponse> future = new CompletableFuture<>();
            future.completeExceptionally(SdkClientException.create(errorMessage));
            return future;
        }

        DeleteMessageRequest.Builder deleteMessageRequestBuilder = deleteMessageRequest.toBuilder();
        appendUserAgent(deleteMessageRequestBuilder);

        String receiptHandle = deleteMessageRequest.receiptHandle();
        String origReceiptHandle = receiptHandle;
        String messagePointer = null;

        // Update original receipt handle if needed.
        if (clientConfiguration.isPayloadSupportEnabled() && isS3ReceiptHandle(receiptHandle)) {
            origReceiptHandle = getOrigReceiptHandle(receiptHandle);

            // Delete pay load from S3 if needed
            if (clientConfiguration.doesCleanupS3Payload()) {
                messagePointer = getMessagePointerFromModifiedReceiptHandle(receiptHandle);
            }
        }

        // The actual message to delete from SQS.
        deleteMessageRequestBuilder.receiptHandle(origReceiptHandle);

        // Check if message is in S3 or only in SQS.
        if (messagePointer == null) {
            // Delete only from SQS
            return super.deleteMessage(deleteMessageRequestBuilder.build());
        }

        // Delete from SQS first, then S3.
        final String messageToDeletePointer = messagePointer;
        return super.deleteMessage(deleteMessageRequestBuilder.build())
            .thenCompose(deleteMessageResponse ->
                payloadStore.deleteOriginalPayload(messageToDeletePointer)
                    .thenApply(v -> deleteMessageResponse));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(
        ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {

        ChangeMessageVisibilityRequest.Builder changeMessageVisibilityRequestBuilder =
            changeMessageVisibilityRequest.toBuilder();
        if (isS3ReceiptHandle(changeMessageVisibilityRequest.receiptHandle())) {
            changeMessageVisibilityRequestBuilder.receiptHandle(
                getOrigReceiptHandle(changeMessageVisibilityRequest.receiptHandle()));
        }
        return amazonSqsToBeExtended.changeMessageVisibility(changeMessageVisibilityRequestBuilder.build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<SendMessageBatchResponse> sendMessageBatch(
        SendMessageBatchRequest sendMessageBatchRequestIn) {

        if (sendMessageBatchRequestIn == null) {
            String errorMessage = "sendMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            CompletableFuture<SendMessageBatchResponse> futureEx = new CompletableFuture<>();
            futureEx.completeExceptionally(SdkClientException.create(errorMessage));
            return futureEx;
        }

        SendMessageBatchRequest.Builder sendMessageBatchRequestBuilder = sendMessageBatchRequestIn.toBuilder();
        appendUserAgent(sendMessageBatchRequestBuilder);
        SendMessageBatchRequest sendMessageBatchRequest = sendMessageBatchRequestBuilder.build();

        if (!clientConfiguration.isPayloadSupportEnabled()) {
            return super.sendMessageBatch(sendMessageBatchRequest);
        }

        List<CompletableFuture<SendMessageBatchRequestEntry>> batchEntryFutures = new ArrayList<>(
            sendMessageBatchRequest.entries().size());
        boolean hasS3Entries = false;
        for (SendMessageBatchRequestEntry entry : sendMessageBatchRequest.entries()) {
            //Check message attributes for ExtendedClient related constraints
            try {
                checkMessageAttributes(clientConfiguration.getPayloadSizeThreshold(), entry.messageAttributes());
            } catch (SdkClientException e) {
                CompletableFuture<SendMessageBatchResponse> futureEx = new CompletableFuture<>();
                futureEx.completeExceptionally(e);
                return futureEx;
            }

            if (clientConfiguration.isAlwaysThroughS3()
                || isLarge(clientConfiguration.getPayloadSizeThreshold(), entry)) {
                batchEntryFutures.add(storeMessageInS3(entry));
                hasS3Entries = true;
            } else {
                batchEntryFutures.add(CompletableFuture.completedFuture(entry));
            }
        }

        if (!hasS3Entries) {
            return super.sendMessageBatch(sendMessageBatchRequest);
        }

        // Convert list of entry futures to a future list of entries.
        return CompletableFuture.allOf(
                batchEntryFutures.toArray(new CompletableFuture[batchEntryFutures.size()]))
            .thenApply(v -> batchEntryFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()))
            .thenCompose(batchEntries -> {
                SendMessageBatchRequest modifiedBatchRequest =
                    sendMessageBatchRequest.toBuilder().entries(batchEntries).build();
                return super.sendMessageBatch(modifiedBatchRequest);
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<DeleteMessageBatchResponse> deleteMessageBatch(
        DeleteMessageBatchRequest deleteMessageBatchRequest) {

        if (deleteMessageBatchRequest == null) {
            String errorMessage = "deleteMessageBatchRequest cannot be null.";
            LOG.error(errorMessage);
            CompletableFuture<DeleteMessageBatchResponse> futureEx = new CompletableFuture<>();
            futureEx.completeExceptionally(SdkClientException.create(errorMessage));
            return futureEx;
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
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatch(
        ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
        List<ChangeMessageVisibilityBatchRequestEntry> entries = new ArrayList<>(
            changeMessageVisibilityBatchRequest.entries().size());
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
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<PurgeQueueResponse> purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        LOG.warn("Calling purgeQueue deletes SQS messages without deleting their payload from S3.");

        if (purgeQueueRequest == null) {
            String errorMessage = "purgeQueueRequest cannot be null.";
            LOG.error(errorMessage);
            CompletableFuture<PurgeQueueResponse> futureEx = new CompletableFuture<>();
            futureEx.completeExceptionally(SdkClientException.create(errorMessage));
            return futureEx;
        }

        PurgeQueueRequest.Builder purgeQueueRequestBuilder = purgeQueueRequest.toBuilder();
        appendUserAgent(purgeQueueRequestBuilder);

        return super.purgeQueue(purgeQueueRequestBuilder.build());
    }

    private CompletableFuture<SendMessageBatchRequestEntry> storeMessageInS3(SendMessageBatchRequestEntry batchEntry) {
        // Read the content of the message from message body
        String messageContentStr = batchEntry.messageBody();

        Long messageContentSize = Util.getStringSizeInBytes(messageContentStr);

        SendMessageBatchRequestEntry.Builder batchEntryBuilder = batchEntry.toBuilder();

        batchEntryBuilder.messageAttributes(
            updateMessageAttributePayloadSize(batchEntry.messageAttributes(), messageContentSize,
                clientConfiguration.usesLegacyReservedAttributeName()));

        // Store the message content in S3.
        return payloadStore.storeOriginalPayload(messageContentStr)
            .thenApply(largeMessagePointer -> {
                batchEntryBuilder.messageBody(largeMessagePointer);
                return batchEntryBuilder.build();
            });
    }

    private CompletableFuture<SendMessageRequest> storeMessageInS3(SendMessageRequest sendMessageRequest) {
        // Read the content of the message from message body
        String messageContentStr = sendMessageRequest.messageBody();

        Long messageContentSize = Util.getStringSizeInBytes(messageContentStr);

        SendMessageRequest.Builder sendMessageRequestBuilder = sendMessageRequest.toBuilder();

        sendMessageRequestBuilder.messageAttributes(
            updateMessageAttributePayloadSize(sendMessageRequest.messageAttributes(), messageContentSize,
                clientConfiguration.usesLegacyReservedAttributeName()));

        // Store the message content in S3.
        return payloadStore.storeOriginalPayload(messageContentStr)
            .thenApply(largeMessagePointer -> {
                sendMessageRequestBuilder.messageBody(largeMessagePointer);
                return sendMessageRequestBuilder.build();
            });
    }

    private static <T extends AwsRequest.Builder> T appendUserAgent(final T builder) {
        return AmazonSQSExtendedClientUtil.appendUserAgent(builder, USER_AGENT_NAME, USER_AGENT_VERSION);
    }
}
