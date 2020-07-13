package com.amazon.sqs.javamessaging;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsync;
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
import com.amazonaws.services.sqs.model.ListQueueTagsRequest;
import com.amazonaws.services.sqs.model.ListQueueTagsResult;
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
import com.amazonaws.services.sqs.model.TagQueueRequest;
import com.amazonaws.services.sqs.model.TagQueueResult;
import com.amazonaws.services.sqs.model.UntagQueueRequest;
import com.amazonaws.services.sqs.model.UntagQueueResult;

/**
 * This is an extended copy of {@link AmazonSQSExtendedClientBase}, with additions to S3 related 
 * code changes for spring-cloud-aws
 * 
 * Read this link: https://github.com/spring-cloud/spring-cloud-aws/issues/167
 * 
 * @author vmehta
 *
 */
abstract class AmazonSQSAsyncExtendedClientBase extends AmazonSQSExtendedClient implements AmazonSQSAsync {

  AmazonSQSAsync amazonSqsToBeExtended;

  public AmazonSQSAsyncExtendedClientBase(AmazonSQSAsync sqsClient) {
    super(sqsClient);
    amazonSqsToBeExtended = sqsClient;
  }

  public Future<AddPermissionResult> addPermissionAsync(AddPermissionRequest addPermissionRequest) {
    return amazonSqsToBeExtended.addPermissionAsync(addPermissionRequest);
  }

  public Future<AddPermissionResult> addPermissionAsync(AddPermissionRequest addPermissionRequest,
      AsyncHandler<AddPermissionRequest, AddPermissionResult> asyncHandler) {
    return amazonSqsToBeExtended.addPermissionAsync(addPermissionRequest, asyncHandler);
  }

  public Future<AddPermissionResult> addPermissionAsync(String queueUrl, String label, List<String> aWSAccountIds,
      List<String> actions) {
    return amazonSqsToBeExtended.addPermissionAsync(queueUrl, label, aWSAccountIds, actions);
  }

  public Future<AddPermissionResult> addPermissionAsync(String queueUrl, String label, List<String> aWSAccountIds,
      List<String> actions, AsyncHandler<AddPermissionRequest, AddPermissionResult> asyncHandler) {
    return amazonSqsToBeExtended.addPermissionAsync(queueUrl, label, aWSAccountIds, actions, asyncHandler);
  }

  public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(
      ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {
    return amazonSqsToBeExtended.changeMessageVisibilityAsync(changeMessageVisibilityRequest);
  }

  public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(
      ChangeMessageVisibilityRequest changeMessageVisibilityRequest,
      AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> asyncHandler) {
    return amazonSqsToBeExtended.changeMessageVisibilityAsync(changeMessageVisibilityRequest, asyncHandler);
  }

  public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(String queueUrl, String receiptHandle,
      Integer visibilityTimeout) {
    return amazonSqsToBeExtended.changeMessageVisibilityAsync(queueUrl, receiptHandle, visibilityTimeout);
  }

  public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(String queueUrl, String receiptHandle,
      Integer visibilityTimeout,
      AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> asyncHandler) {
    return amazonSqsToBeExtended.changeMessageVisibilityAsync(queueUrl, receiptHandle, visibilityTimeout, asyncHandler);
  }

  public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(
      ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
    return amazonSqsToBeExtended.changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest);
  }

  public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(
      ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest,
      AsyncHandler<ChangeMessageVisibilityBatchRequest, ChangeMessageVisibilityBatchResult> asyncHandler) {
    return amazonSqsToBeExtended.changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest, asyncHandler);
  }

  public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(String queueUrl,
      List<ChangeMessageVisibilityBatchRequestEntry> entries) {
    return amazonSqsToBeExtended.changeMessageVisibilityBatchAsync(queueUrl, entries);
  }

  public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(String queueUrl,
      List<ChangeMessageVisibilityBatchRequestEntry> entries,
      AsyncHandler<ChangeMessageVisibilityBatchRequest, ChangeMessageVisibilityBatchResult> asyncHandler) {
    return amazonSqsToBeExtended.changeMessageVisibilityBatchAsync(queueUrl, entries, asyncHandler);
  }

  public Future<CreateQueueResult> createQueueAsync(CreateQueueRequest createQueueRequest) {
    return amazonSqsToBeExtended.createQueueAsync(createQueueRequest);
  }

  public Future<CreateQueueResult> createQueueAsync(CreateQueueRequest createQueueRequest,
      AsyncHandler<CreateQueueRequest, CreateQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.createQueueAsync(createQueueRequest, asyncHandler);
  }

  public Future<CreateQueueResult> createQueueAsync(String queueName) {
    return amazonSqsToBeExtended.createQueueAsync(queueName);
  }

  public Future<CreateQueueResult> createQueueAsync(String queueName,
      AsyncHandler<CreateQueueRequest, CreateQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.createQueueAsync(queueName, asyncHandler);
  }

  public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest) {
    return amazonSqsToBeExtended.deleteMessageAsync(deleteMessageRequest);
  }

  public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest,
      AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler) {
    return amazonSqsToBeExtended.deleteMessageAsync(deleteMessageRequest, asyncHandler);
  }

  public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl, String receiptHandle) {
    return amazonSqsToBeExtended.deleteMessageAsync(queueUrl, receiptHandle);
  }

  public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl, String receiptHandle,
      AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler) {
    return amazonSqsToBeExtended.deleteMessageAsync(queueUrl, receiptHandle, asyncHandler);
  }

  public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(DeleteMessageBatchRequest deleteMessageBatchRequest) {
    return amazonSqsToBeExtended.deleteMessageBatchAsync(deleteMessageBatchRequest);
  }

  public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(DeleteMessageBatchRequest deleteMessageBatchRequest,
      AsyncHandler<DeleteMessageBatchRequest, DeleteMessageBatchResult> asyncHandler) {
    return amazonSqsToBeExtended.deleteMessageBatchAsync(deleteMessageBatchRequest, asyncHandler);
  }

  public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(String queueUrl,
      List<DeleteMessageBatchRequestEntry> entries) {
    return amazonSqsToBeExtended.deleteMessageBatchAsync(queueUrl, entries);
  }

  public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(String queueUrl,
      List<DeleteMessageBatchRequestEntry> entries,
      AsyncHandler<DeleteMessageBatchRequest, DeleteMessageBatchResult> asyncHandler) {
    return amazonSqsToBeExtended.deleteMessageBatchAsync(queueUrl, entries, asyncHandler);
  }

  public Future<DeleteQueueResult> deleteQueueAsync(DeleteQueueRequest deleteQueueRequest) {
    return amazonSqsToBeExtended.deleteQueueAsync(deleteQueueRequest);
  }

  public Future<DeleteQueueResult> deleteQueueAsync(DeleteQueueRequest deleteQueueRequest,
      AsyncHandler<DeleteQueueRequest, DeleteQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.deleteQueueAsync(deleteQueueRequest, asyncHandler);
  }

  public Future<DeleteQueueResult> deleteQueueAsync(String queueUrl) {
    return amazonSqsToBeExtended.deleteQueueAsync(queueUrl);
  }

  public Future<DeleteQueueResult> deleteQueueAsync(String queueUrl,
      AsyncHandler<DeleteQueueRequest, DeleteQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.deleteQueueAsync(queueUrl, asyncHandler);
  }

  public Future<GetQueueAttributesResult> getQueueAttributesAsync(GetQueueAttributesRequest getQueueAttributesRequest) {
    return amazonSqsToBeExtended.getQueueAttributesAsync(getQueueAttributesRequest);
  }

  public Future<GetQueueAttributesResult> getQueueAttributesAsync(GetQueueAttributesRequest getQueueAttributesRequest,
      AsyncHandler<GetQueueAttributesRequest, GetQueueAttributesResult> asyncHandler) {
    return amazonSqsToBeExtended.getQueueAttributesAsync(getQueueAttributesRequest, asyncHandler);
  }

  public Future<GetQueueAttributesResult> getQueueAttributesAsync(String queueUrl, List<String> attributeNames) {
    return amazonSqsToBeExtended.getQueueAttributesAsync(queueUrl, attributeNames);
  }

  public Future<GetQueueAttributesResult> getQueueAttributesAsync(String queueUrl, List<String> attributeNames,
      AsyncHandler<GetQueueAttributesRequest, GetQueueAttributesResult> asyncHandler) {
    return amazonSqsToBeExtended.getQueueAttributesAsync(queueUrl, attributeNames, asyncHandler);
  }

  public Future<GetQueueUrlResult> getQueueUrlAsync(GetQueueUrlRequest getQueueUrlRequest) {
    return amazonSqsToBeExtended.getQueueUrlAsync(getQueueUrlRequest);
  }

  public Future<GetQueueUrlResult> getQueueUrlAsync(GetQueueUrlRequest getQueueUrlRequest,
      AsyncHandler<GetQueueUrlRequest, GetQueueUrlResult> asyncHandler) {
    return amazonSqsToBeExtended.getQueueUrlAsync(getQueueUrlRequest, asyncHandler);
  }

  public Future<GetQueueUrlResult> getQueueUrlAsync(String queueName) {
    return amazonSqsToBeExtended.getQueueUrlAsync(queueName);
  }

  public Future<GetQueueUrlResult> getQueueUrlAsync(String queueName,
      AsyncHandler<GetQueueUrlRequest, GetQueueUrlResult> asyncHandler) {
    return amazonSqsToBeExtended.getQueueUrlAsync(queueName, asyncHandler);
  }

  public Future<ListDeadLetterSourceQueuesResult> listDeadLetterSourceQueuesAsync(
      ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) {
    return amazonSqsToBeExtended.listDeadLetterSourceQueuesAsync(listDeadLetterSourceQueuesRequest);
  }

  public Future<ListDeadLetterSourceQueuesResult> listDeadLetterSourceQueuesAsync(
      ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest,
      AsyncHandler<ListDeadLetterSourceQueuesRequest, ListDeadLetterSourceQueuesResult> asyncHandler) {
    return amazonSqsToBeExtended.listDeadLetterSourceQueuesAsync(listDeadLetterSourceQueuesRequest, asyncHandler);
  }

  public Future<ListQueueTagsResult> listQueueTagsAsync(ListQueueTagsRequest listQueueTagsRequest) {
    return amazonSqsToBeExtended.listQueueTagsAsync(listQueueTagsRequest);
  }

  public Future<ListQueueTagsResult> listQueueTagsAsync(ListQueueTagsRequest listQueueTagsRequest,
      AsyncHandler<ListQueueTagsRequest, ListQueueTagsResult> asyncHandler) {
    return amazonSqsToBeExtended.listQueueTagsAsync(listQueueTagsRequest, asyncHandler);
  }

  public Future<ListQueueTagsResult> listQueueTagsAsync(String queueUrl) {
    return amazonSqsToBeExtended.listQueueTagsAsync(queueUrl);
  }

  public Future<ListQueueTagsResult> listQueueTagsAsync(String queueUrl,
      AsyncHandler<ListQueueTagsRequest, ListQueueTagsResult> asyncHandler) {
    return amazonSqsToBeExtended.listQueueTagsAsync(queueUrl, asyncHandler);
  }

  public Future<ListQueuesResult> listQueuesAsync(ListQueuesRequest listQueuesRequest) {
    return amazonSqsToBeExtended.listQueuesAsync(listQueuesRequest);
  }

  public Future<ListQueuesResult> listQueuesAsync(ListQueuesRequest listQueuesRequest,
      AsyncHandler<ListQueuesRequest, ListQueuesResult> asyncHandler) {
    return amazonSqsToBeExtended.listQueuesAsync(listQueuesRequest, asyncHandler);
  }

  public Future<ListQueuesResult> listQueuesAsync() {
    return amazonSqsToBeExtended.listQueuesAsync();
  }

  public Future<ListQueuesResult> listQueuesAsync(AsyncHandler<ListQueuesRequest, ListQueuesResult> asyncHandler) {
    return amazonSqsToBeExtended.listQueuesAsync(asyncHandler);
  }

  public Future<ListQueuesResult> listQueuesAsync(String queueNamePrefix) {
    return amazonSqsToBeExtended.listQueuesAsync(queueNamePrefix);
  }

  public Future<ListQueuesResult> listQueuesAsync(String queueNamePrefix,
      AsyncHandler<ListQueuesRequest, ListQueuesResult> asyncHandler) {
    return amazonSqsToBeExtended.listQueuesAsync(queueNamePrefix, asyncHandler);
  }

  public Future<PurgeQueueResult> purgeQueueAsync(PurgeQueueRequest purgeQueueRequest) {
    return amazonSqsToBeExtended.purgeQueueAsync(purgeQueueRequest);
  }

  public Future<PurgeQueueResult> purgeQueueAsync(PurgeQueueRequest purgeQueueRequest,
      AsyncHandler<PurgeQueueRequest, PurgeQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.purgeQueueAsync(purgeQueueRequest, asyncHandler);
  }

  public Future<ReceiveMessageResult> receiveMessageAsync(ReceiveMessageRequest receiveMessageRequest) {
    return amazonSqsToBeExtended.receiveMessageAsync(receiveMessageRequest);
  }

  public Future<ReceiveMessageResult> receiveMessageAsync(ReceiveMessageRequest receiveMessageRequest,
      AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> asyncHandler) {
    return amazonSqsToBeExtended.receiveMessageAsync(receiveMessageRequest, asyncHandler);
  }

  public Future<ReceiveMessageResult> receiveMessageAsync(String queueUrl) {
    return amazonSqsToBeExtended.receiveMessageAsync(queueUrl);
  }

  public Future<ReceiveMessageResult> receiveMessageAsync(String queueUrl,
      AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> asyncHandler) {
    return amazonSqsToBeExtended.receiveMessageAsync(queueUrl, asyncHandler);
  }

  public Future<RemovePermissionResult> removePermissionAsync(RemovePermissionRequest removePermissionRequest) {
    return amazonSqsToBeExtended.removePermissionAsync(removePermissionRequest);
  }

  public Future<RemovePermissionResult> removePermissionAsync(RemovePermissionRequest removePermissionRequest,
      AsyncHandler<RemovePermissionRequest, RemovePermissionResult> asyncHandler) {
    return amazonSqsToBeExtended.removePermissionAsync(removePermissionRequest, asyncHandler);
  }

  public Future<RemovePermissionResult> removePermissionAsync(String queueUrl, String label) {
    return amazonSqsToBeExtended.removePermissionAsync(queueUrl, label);
  }

  public Future<RemovePermissionResult> removePermissionAsync(String queueUrl, String label,
      AsyncHandler<RemovePermissionRequest, RemovePermissionResult> asyncHandler) {
    return amazonSqsToBeExtended.removePermissionAsync(queueUrl, label, asyncHandler);
  }

  public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest) {
    return amazonSqsToBeExtended.sendMessageAsync(sendMessageRequest);
  }

  public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest,
      AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler) {
    return amazonSqsToBeExtended.sendMessageAsync(sendMessageRequest, asyncHandler);
  }

  public Future<SendMessageResult> sendMessageAsync(String queueUrl, String messageBody) {
    return amazonSqsToBeExtended.sendMessageAsync(queueUrl, messageBody);
  }

  public Future<SendMessageResult> sendMessageAsync(String queueUrl, String messageBody,
      AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler) {
    return amazonSqsToBeExtended.sendMessageAsync(queueUrl, messageBody, asyncHandler);
  }

  public Future<SendMessageBatchResult> sendMessageBatchAsync(SendMessageBatchRequest sendMessageBatchRequest) {
    return amazonSqsToBeExtended.sendMessageBatchAsync(sendMessageBatchRequest);
  }

  public Future<SendMessageBatchResult> sendMessageBatchAsync(SendMessageBatchRequest sendMessageBatchRequest,
      AsyncHandler<SendMessageBatchRequest, SendMessageBatchResult> asyncHandler) {
    return amazonSqsToBeExtended.sendMessageBatchAsync(sendMessageBatchRequest, asyncHandler);
  }

  public Future<SendMessageBatchResult> sendMessageBatchAsync(String queueUrl,
      List<SendMessageBatchRequestEntry> entries) {
    return amazonSqsToBeExtended.sendMessageBatchAsync(queueUrl, entries);
  }

  public Future<SendMessageBatchResult> sendMessageBatchAsync(String queueUrl,
      List<SendMessageBatchRequestEntry> entries,
      AsyncHandler<SendMessageBatchRequest, SendMessageBatchResult> asyncHandler) {
    return amazonSqsToBeExtended.sendMessageBatchAsync(queueUrl, entries, asyncHandler);
  }

  public Future<SetQueueAttributesResult> setQueueAttributesAsync(SetQueueAttributesRequest setQueueAttributesRequest) {
    return amazonSqsToBeExtended.setQueueAttributesAsync(setQueueAttributesRequest);
  }

  public Future<SetQueueAttributesResult> setQueueAttributesAsync(SetQueueAttributesRequest setQueueAttributesRequest,
      AsyncHandler<SetQueueAttributesRequest, SetQueueAttributesResult> asyncHandler) {
    return amazonSqsToBeExtended.setQueueAttributesAsync(setQueueAttributesRequest, asyncHandler);
  }

  public Future<SetQueueAttributesResult> setQueueAttributesAsync(String queueUrl, Map<String, String> attributes) {
    return amazonSqsToBeExtended.setQueueAttributesAsync(queueUrl, attributes);
  }

  public Future<SetQueueAttributesResult> setQueueAttributesAsync(String queueUrl, Map<String, String> attributes,
      AsyncHandler<SetQueueAttributesRequest, SetQueueAttributesResult> asyncHandler) {
    return amazonSqsToBeExtended.setQueueAttributesAsync(queueUrl, attributes, asyncHandler);
  }

  public Future<TagQueueResult> tagQueueAsync(TagQueueRequest tagQueueRequest) {
    return amazonSqsToBeExtended.tagQueueAsync(tagQueueRequest);
  }

  public Future<TagQueueResult> tagQueueAsync(TagQueueRequest tagQueueRequest,
      AsyncHandler<TagQueueRequest, TagQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.tagQueueAsync(tagQueueRequest, asyncHandler);
  }

  public Future<TagQueueResult> tagQueueAsync(String queueUrl, Map<String, String> tags) {
    return amazonSqsToBeExtended.tagQueueAsync(queueUrl, tags);
  }

  public Future<TagQueueResult> tagQueueAsync(String queueUrl, Map<String, String> tags,
      AsyncHandler<TagQueueRequest, TagQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.tagQueueAsync(queueUrl, tags, asyncHandler);
  }

  public Future<UntagQueueResult> untagQueueAsync(UntagQueueRequest untagQueueRequest) {
    return amazonSqsToBeExtended.untagQueueAsync(untagQueueRequest);
  }

  public Future<UntagQueueResult> untagQueueAsync(UntagQueueRequest untagQueueRequest,
      AsyncHandler<UntagQueueRequest, UntagQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.untagQueueAsync(untagQueueRequest, asyncHandler);
  }

  public Future<UntagQueueResult> untagQueueAsync(String queueUrl, List<String> tagKeys) {
    return amazonSqsToBeExtended.untagQueueAsync(queueUrl, tagKeys);
  }

  public Future<UntagQueueResult> untagQueueAsync(String queueUrl, List<String> tagKeys,
      AsyncHandler<UntagQueueRequest, UntagQueueResult> asyncHandler) {
    return amazonSqsToBeExtended.untagQueueAsync(queueUrl, tagKeys, asyncHandler);
  }

}
