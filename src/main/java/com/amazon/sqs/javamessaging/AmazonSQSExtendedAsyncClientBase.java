package com.amazon.sqs.javamessaging;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsRequest;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.RemovePermissionResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.TagQueueRequest;
import software.amazon.awssdk.services.sqs.model.TagQueueResponse;
import software.amazon.awssdk.services.sqs.model.UntagQueueRequest;
import software.amazon.awssdk.services.sqs.model.UntagQueueResponse;

abstract class AmazonSQSExtendedAsyncClientBase implements SqsAsyncClient {
    SqsAsyncClient amazonSqsToBeExtended;

    public AmazonSQSExtendedAsyncClientBase(SqsAsyncClient sqsClient) {
        amazonSqsToBeExtended = sqsClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        return amazonSqsToBeExtended.sendMessage(sendMessageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        return amazonSqsToBeExtended.receiveMessage(receiveMessageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        return amazonSqsToBeExtended.deleteMessage(deleteMessageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<SetQueueAttributesResponse> setQueueAttributes(
        SetQueueAttributesRequest setQueueAttributesRequest) {
        return amazonSqsToBeExtended.setQueueAttributes(setQueueAttributesRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatch(
        ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
        return amazonSqsToBeExtended.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(
        ChangeMessageVisibilityRequest changeMessageVisibilityRequest)  {
        return amazonSqsToBeExtended.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<GetQueueUrlResponse> getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        return amazonSqsToBeExtended.getQueueUrl(getQueueUrlRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<RemovePermissionResponse> removePermission(
        RemovePermissionRequest removePermissionRequest) {
        return amazonSqsToBeExtended.removePermission(removePermissionRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<GetQueueAttributesResponse> getQueueAttributes(
        GetQueueAttributesRequest getQueueAttributesRequest) {
        return amazonSqsToBeExtended.getQueueAttributes(getQueueAttributesRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<SendMessageBatchResponse> sendMessageBatch(
        SendMessageBatchRequest sendMessageBatchRequest) {
        return amazonSqsToBeExtended.sendMessageBatch(sendMessageBatchRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<PurgeQueueResponse> purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        return amazonSqsToBeExtended.purgeQueue(purgeQueueRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ListDeadLetterSourceQueuesResponse> listDeadLetterSourceQueues(
        ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) {
        return amazonSqsToBeExtended.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<DeleteQueueResponse> deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        return amazonSqsToBeExtended.deleteQueue(deleteQueueRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ListQueuesResponse> listQueues(ListQueuesRequest listQueuesRequest) {
        return amazonSqsToBeExtended.listQueues(listQueuesRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ListQueuesResponse> listQueues() {
        return amazonSqsToBeExtended.listQueues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<DeleteMessageBatchResponse> deleteMessageBatch(
        DeleteMessageBatchRequest deleteMessageBatchRequest) {
        return amazonSqsToBeExtended.deleteMessageBatch(deleteMessageBatchRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<CreateQueueResponse> createQueue(CreateQueueRequest createQueueRequest)  {
        return amazonSqsToBeExtended.createQueue(createQueueRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AddPermissionResponse> addPermission(AddPermissionRequest addPermissionRequest) {
        return amazonSqsToBeExtended.addPermission(addPermissionRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ListQueueTagsResponse> listQueueTags(final ListQueueTagsRequest listQueueTagsRequest) {
        return amazonSqsToBeExtended.listQueueTags(listQueueTagsRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<TagQueueResponse> tagQueue(final TagQueueRequest tagQueueRequest) {
        return amazonSqsToBeExtended.tagQueue(tagQueueRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<UntagQueueResponse> untagQueue(final UntagQueueRequest untagQueueRequest) {
        return amazonSqsToBeExtended.untagQueue(untagQueueRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serviceName() {
        return amazonSqsToBeExtended.serviceName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        amazonSqsToBeExtended.close();
    }
}
