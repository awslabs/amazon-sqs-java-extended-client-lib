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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.BatchEntryIdsNotDistinctException;
import software.amazon.awssdk.services.sqs.model.BatchRequestTooLongException;
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
import software.amazon.awssdk.services.sqs.model.EmptyBatchRequestException;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.InvalidAttributeNameException;
import software.amazon.awssdk.services.sqs.model.InvalidBatchEntryIdException;
import software.amazon.awssdk.services.sqs.model.InvalidIdFormatException;
import software.amazon.awssdk.services.sqs.model.InvalidMessageContentsException;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsRequest;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.MessageNotInflightException;
import software.amazon.awssdk.services.sqs.model.OverLimitException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueInProgressException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.QueueDeletedRecentlyException;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
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
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sqs.model.TagQueueRequest;
import software.amazon.awssdk.services.sqs.model.TagQueueResponse;
import software.amazon.awssdk.services.sqs.model.TooManyEntriesInBatchRequestException;
import software.amazon.awssdk.services.sqs.model.UntagQueueRequest;
import software.amazon.awssdk.services.sqs.model.UntagQueueResponse;

abstract class AmazonSQSExtendedClientBase implements SqsClient {
    SqsClient amazonSqsToBeExtended;

    protected AmazonSQSExtendedClientBase(SqsClient sqsClient) {
        amazonSqsToBeExtended = sqsClient;
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
    @Override
    public SendMessageResponse sendMessage(SendMessageRequest sendMessageRequest) throws
            InvalidMessageContentsException, UnsupportedOperationException, SdkException, SdkClientException, SqsException {
        return amazonSqsToBeExtended.sendMessage(sendMessageRequest);
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
    @Override
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        return amazonSqsToBeExtended.receiveMessage(receiveMessageRequest);
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
    @Override
    public DeleteMessageResponse deleteMessage(DeleteMessageRequest deleteMessageRequest) throws
            InvalidIdFormatException, ReceiptHandleIsInvalidException, SdkException, SdkClientException, SqsException {
        return amazonSqsToBeExtended.deleteMessage(deleteMessageRequest);
    }

    /**
     * <p>
     * Sets the value of one or more queue attributes. When you change a queue's attributes, the change can take up to
     * 60 seconds for most of the attributes to propagate throughout the Amazon SQS system. Changes made to the
     * <code>MessageRetentionPeriod</code> attribute can take up to 15 minutes.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * In the future, new attributes might be added. If you write code that calls this action, we recommend that you
     * structure your code so that it can handle new attributes gracefully.
     * </p>
     * </li>
     * <li>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note>
     *
     * @param setQueueAttributesRequest
     * @return Result of the SetQueueAttributes operation returned by the service.
     * @throws InvalidAttributeNameException
     *         The specified attribute doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.SetQueueAttributes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/SetQueueAttributes" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public SetQueueAttributesResponse setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest)
            throws AwsServiceException, SdkClientException, SqsException {

        return amazonSqsToBeExtended.setQueueAttributes(setQueueAttributesRequest);
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
    @Override
    public ChangeMessageVisibilityBatchResponse changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) throws AwsServiceException,
            SdkClientException {

        return amazonSqsToBeExtended.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
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
    @Override
    public ChangeMessageVisibilityResponse changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    /**
     * <p>
     * Returns the URL of an existing Amazon SQS queue.
     * </p>
     * <p>
     * To access a queue that belongs to another AWS account, use the <code>QueueOwnerAWSAccountId</code> parameter to
     * specify the account ID of the queue's owner. The queue's owner must grant you permission to access the queue. For
     * more information about shared queue access, see <code> <a>AddPermission</a> </code> or see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-writing-an-sqs-policy.html#write-messages-to-shared-queue"
     * >Allow Developers to Write Messages to a Shared Queue</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     *
     * @param getQueueUrlRequest
     * @return Result of the GetQueueUrl operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.GetQueueUrl
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/GetQueueUrl" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public GetQueueUrlResponse getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) throws AwsServiceException,
            SdkClientException {

        return amazonSqsToBeExtended.getQueueUrl(getQueueUrlRequest);
    }

    /**
     * <p>
     * Revokes any permissions in the queue policy that matches the specified <code>Label</code> parameter.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * Only the owner of a queue can remove permissions from it.
     * </p>
     * </li>
     * <li>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note>
     *
     * @param removePermissionRequest
     * @return Result of the RemovePermission operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.RemovePermission
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/RemovePermission" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public RemovePermissionResponse removePermission(RemovePermissionRequest removePermissionRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.removePermission(removePermissionRequest);
    }

    /**
     * <p>
     * Gets attributes for the specified queue.
     * </p>
     * <note>
     * <p>
     * To determine whether a queue is <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html">FIFO</a>, you
     * can check whether <code>QueueName</code> ends with the <code>.fifo</code> suffix.
     * </p>
     * </note>
     *
     * @param getQueueAttributesRequest
     * @return Result of the GetQueueAttributes operation returned by the service.
     * @throws InvalidAttributeNameException
     *         The specified attribute doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.GetQueueAttributes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/GetQueueAttributes" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public GetQueueAttributesResponse getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.getQueueAttributes(getQueueAttributesRequest);
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
    @Override
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.sendMessageBatch(sendMessageBatchRequest);
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
    @Override
    public PurgeQueueResponse purgeQueue(PurgeQueueRequest purgeQueueRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.purgeQueue(purgeQueueRequest);

    }

    /**
     * <p>
     * Returns a list of your queues that have the <code>RedrivePolicy</code> queue attribute configured with a
     * dead-letter queue.
     * </p>
     * <p>
     * The <code>ListDeadLetterSourceQueues</code> methods supports pagination. Set parameter <code>MaxResults</code> in
     * the request to specify the maximum number of results to be returned in the response. If you do not set
     * <code>MaxResults</code>, the response includes a maximum of 1,000 results. If you set <code>MaxResults</code> and
     * there are additional results to display, the response includes a value for <code>NextToken</code>. Use
     * <code>NextToken</code> as a parameter in your next request to <code>ListDeadLetterSourceQueues</code> to receive
     * the next page of results.
     * </p>
     * <p>
     * For more information about using dead-letter queues, see <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html"
     * >Using Amazon SQS Dead-Letter Queues</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     *
     * @param listDeadLetterSourceQueuesRequest
     * @return Result of the ListDeadLetterSourceQueues operation returned by the service.
     * @throws QueueDoesNotExistException
     *         The specified queue doesn't exist.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListDeadLetterSourceQueues
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListDeadLetterSourceQueues" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public ListDeadLetterSourceQueuesResponse listDeadLetterSourceQueues(
            ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) throws AwsServiceException,
            SdkClientException {

        return amazonSqsToBeExtended.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    /**
     * <p>
     * Deletes the queue specified by the <code>QueueUrl</code>, regardless of the queue's contents.
     * </p>
     * <important>
     * <p>
     * Be careful with the <code>DeleteQueue</code> action: When you delete a queue, any messages in the queue are no
     * longer available.
     * </p>
     * </important>
     * <p>
     * When you delete a queue, the deletion process takes up to 60 seconds. Requests you send involving that queue
     * during the 60 seconds might succeed. For example, a <code> <a>SendMessage</a> </code> request might succeed, but
     * after 60 seconds the queue and the message you sent no longer exist.
     * </p>
     * <p>
     * When you delete a queue, you must wait at least 60 seconds before creating a queue with the same name.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param deleteQueueRequest
     * @return Result of the DeleteQueue operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.DeleteQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/DeleteQueue" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public DeleteQueueResponse deleteQueue(DeleteQueueRequest deleteQueueRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.deleteQueue(deleteQueueRequest);
    }

    /**
     * <p>
     * Returns a list of your queues in the current region. The response includes a maximum of 1,000 results. If you
     * specify a value for the optional <code>QueueNamePrefix</code> parameter, only queues with a name that begins with
     * the specified value are returned.
     * </p>
     * <p>
     * The <code>listQueues</code> methods supports pagination. Set parameter <code>MaxResults</code> in the request to
     * specify the maximum number of results to be returned in the response. If you do not set <code>MaxResults</code>,
     * the response includes a maximum of 1,000 results. If you set <code>MaxResults</code> and there are additional
     * results to display, the response includes a value for <code>NextToken</code>. Use <code>NextToken</code> as a
     * parameter in your next request to <code>listQueues</code> to receive the next page of results.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param listQueuesRequest
     * @return Result of the ListQueues operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListQueues
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListQueues" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public ListQueuesResponse listQueues(ListQueuesRequest listQueuesRequest) throws AwsServiceException,
            SdkClientException {

        return amazonSqsToBeExtended.listQueues(listQueuesRequest);
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
    @Override
    public DeleteMessageBatchResponse deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.deleteMessageBatch(deleteMessageBatchRequest);
    }

    /**
     * <p>
     * Creates a new standard or FIFO queue. You can pass one or more attributes in the request. Keep the following in
     * mind:
     * </p>
     * <ul>
     * <li>
     * <p>
     * If you don't specify the <code>FifoQueue</code> attribute, Amazon SQS creates a standard queue.
     * </p>
     * <note>
     * <p>
     * You can't change the queue type after you create it and you can't convert an existing standard queue into a FIFO
     * queue. You must either create a new FIFO queue for your application or delete your existing standard queue and
     * recreate it as a FIFO queue. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html#FIFO-queues-moving"
     * >Moving From a Standard Queue to a FIFO Queue</a> in the <i>Amazon Simple Queue Service Developer Guide</i>.
     * </p>
     * </note></li>
     * <li>
     * <p>
     * If you don't provide a value for an attribute, the queue is created with the default value for the attribute.
     * </p>
     * </li>
     * <li>
     * <p>
     * If you delete a queue, you must wait at least 60 seconds before creating a queue with the same name.
     * </p>
     * </li>
     * </ul>
     * <p>
     * To successfully create a new queue, you must provide a queue name that adheres to the <a
     * href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/limits-queues.html">limits
     * related to queues</a> and is unique within the scope of your queues.
     * </p>
     * <note>
     * <p>
     * After you create a queue, you must wait at least one second after the queue is created to be able to use the
     * queue.
     * </p>
     * </note>
     * <p>
     * To get the queue URL, use the <code> <a>GetQueueUrl</a> </code> action. <code> <a>GetQueueUrl</a> </code>
     * requires only the <code>QueueName</code> parameter. be aware of existing queue names:
     * </p>
     * <ul>
     * <li>
     * <p>
     * If you provide the name of an existing queue along with the exact names and values of all the queue's attributes,
     * <code>CreateQueue</code> returns the queue URL for the existing queue.
     * </p>
     * </li>
     * <li>
     * <p>
     * If the queue name, attribute names, or attribute values don't match an existing queue, <code>CreateQueue</code>
     * returns an error.
     * </p>
     * </li>
     * </ul>
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
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param createQueueRequest
     * @return Result of the CreateQueue operation returned by the service.
     * @throws QueueDeletedRecentlyException
     *         You must wait 60 seconds after deleting a queue before you can create another queue with the same name.
     * @throws QueueNameExistsException
     *         A queue with this name already exists. Amazon SQS returns this error only if the request includes
     *         attributes whose values differ from those of the existing queue.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.CreateQueue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/CreateQueue" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CreateQueueResponse createQueue(CreateQueueRequest createQueueRequest)
            throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.createQueue(createQueueRequest);
    }

    /**
     * <p>
     * Adds a permission to a queue for a specific <a
     * href="https://docs.aws.amazon.com/general/latest/gr/glos-chap.html#P">principal</a>. This allows sharing access
     * to the queue.
     * </p>
     * <p>
     * When you create a queue, you have full control access rights for the queue. Only you, the owner of the queue, can
     * grant or deny permissions to the queue. For more information about these permissions, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-writing-an-sqs-policy.html#write-messages-to-shared-queue"
     * >Allow Developers to Write Messages to a Shared Queue</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * <note>
     * <ul>
     * <li>
     * <p>
     * <code>AddPermission</code> generates a policy for you. You can use <code> <a>SetQueueAttributes</a> </code> to
     * upload your policy. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-creating-custom-policies.html"
     * >Using Custom Policies with the Amazon SQS Access Policy Language</a> in the <i>Amazon Simple Queue Service
     * Developer Guide</i>.
     * </p>
     * </li>
     * <li>
     * <p>
     * An Amazon SQS policy can have a maximum of 7 actions.
     * </p>
     * </li>
     * <li>
     * <p>
     * To remove the ability to change queue permissions, you must deny permission to the <code>AddPermission</code>,
     * <code>RemovePermission</code>, and <code>SetQueueAttributes</code> actions in your IAM policy.
     * </p>
     * </li>
     * </ul>
     * </note>
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
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @param addPermissionRequest
     * @return Result of the AddPermission operation returned by the service.
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
     * @sample SqsClient.AddPermission
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/AddPermission" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public AddPermissionResponse addPermission(AddPermissionRequest addPermissionRequest)
            throws AwsServiceException, SdkClientException, OverLimitException {

        return amazonSqsToBeExtended.addPermission(addPermissionRequest);
    }

    /**
     * <p>
     * Returns a list of your queues in the current region. The response includes a maximum of 1,000 results. If you
     * specify a value for the optional <code>QueueNamePrefix</code> parameter, only queues with a name that begins with
     * the specified value are returned.
     * </p>
     * <p>
     * The <code>listQueues</code> methods supports pagination. Set parameter <code>MaxResults</code> in the request to
     * specify the maximum number of results to be returned in the response. If you do not set <code>MaxResults</code>,
     * the response includes a maximum of 1,000 results. If you set <code>MaxResults</code> and there are additional
     * results to display, the response includes a value for <code>NextToken</code>. Use <code>NextToken</code> as a
     * parameter in your next request to <code>listQueues</code> to receive the next page of results.
     * </p>
     * <note>
     * <p>
     * Cross-account permissions don't apply to this action. For more information, see <a href=
     * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-customer-managed-policy-examples.html#grant-cross-account-permissions-to-role-and-user-name"
     * >Grant Cross-Account Permissions to a Role and a User Name</a> in the <i>Amazon Simple Queue Service Developer
     * Guide</i>.
     * </p>
     * </note>
     *
     * @return Result of the ListQueues operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SqsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SqsClient.ListQueues
     * @see #listQueues(ListQueuesRequest)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/sqs-2012-11-05/ListQueues" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public ListQueuesResponse listQueues() throws AwsServiceException, SdkClientException {

        return amazonSqsToBeExtended.listQueues();
    }

    /** {@inheritDoc} */
    @Override public ListQueueTagsResponse listQueueTags(final ListQueueTagsRequest listQueueTagsRequest) {
        return amazonSqsToBeExtended.listQueueTags(listQueueTagsRequest);
    }

    /** {@inheritDoc} */
    @Override public TagQueueResponse tagQueue(final TagQueueRequest tagQueueRequest) {
        return amazonSqsToBeExtended.tagQueue(tagQueueRequest);
    }

    /** {@inheritDoc} */
    @Override public UntagQueueResponse untagQueue(final UntagQueueRequest untagQueueRequest) {
        return amazonSqsToBeExtended.untagQueue(untagQueueRequest);
    }

    @Override
    public String serviceName() {
        return amazonSqsToBeExtended.serviceName();
    }

    @Override
    public void close() {
        amazonSqsToBeExtended.close();
    }
}
