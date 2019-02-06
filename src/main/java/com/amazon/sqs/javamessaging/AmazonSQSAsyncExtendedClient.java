package com.amazon.sqs.javamessaging;

import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

/**
 * Amazon decided they did not want to work on an extended Async version, so
 * took it up for my project.
 * 
 * https://github.com/awslabs/amazon-sqs-java-extended-client-lib/issues/5
 * https://github.com/spring-cloud/spring-cloud-aws/issues/167
 *
 */
public class AmazonSQSAsyncExtendedClient extends AmazonSQSAsyncExtendedClientBase implements AmazonSQSAsync {
  private static final Log LOG = LogFactory.getLog(AmazonSQSAsyncExtendedClient.class);

  private ExtendedClientConfiguration clientConfiguration;

  /**
   * Constructs a new Amazon SQS extended client to invoke service methods on
   * Amazon SQS with extended functionality using the specified Amazon SQS client
   * object.
   *
   * <p>
   * All service calls made using this new client object are blocking, and will
   * not return until the service call completes.
   *
   * @param sqsClient
   *          The Amazon SQS client to use to connect to Amazon SQS.
   */
  public AmazonSQSAsyncExtendedClient(AmazonSQSAsync sqsClient) {
    this(sqsClient, new ExtendedClientConfiguration());
  }

  /**
   * Constructs a new Amazon SQS extended client to invoke service methods on
   * Amazon SQS with extended functionality using the specified Amazon SQS client
   * object.
   *
   * <p>
   * All service calls made using this new client object are blocking, and will
   * not return until the service call completes.
   *
   * @param sqsClient
   *          The Amazon SQS client to use to connect to Amazon SQS.
   * @param extendedClientConfig
   *          The extended client configuration options controlling the
   *          functionality of this client.
   */
  public AmazonSQSAsyncExtendedClient(AmazonSQSAsync sqsClient, ExtendedClientConfiguration extendedClientConfig) {
    super(sqsClient);
    this.clientConfiguration = new ExtendedClientConfiguration(extendedClientConfig);
  }

  public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest) {
    return sendMessageAsync(sendMessageRequest, null);
  }

  public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest,
      AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler) {

    if (sendMessageRequest == null) {
      String errorMessage = "sendMessageRequest cannot be null.";
      LOG.error(errorMessage);
      throw new AmazonClientException(errorMessage);
    }

    sendMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

    if (!clientConfiguration.isLargePayloadSupportEnabled()) {
      return super.sendMessageAsync(sendMessageRequest);
    }

    if (sendMessageRequest.getMessageBody() == null || "".equals(sendMessageRequest.getMessageBody())) {
      String errorMessage = "messageBody cannot be null or empty.";
      LOG.error(errorMessage);
      throw new AmazonClientException(errorMessage);
    }

    if (clientConfiguration.isAlwaysThroughS3() || isLarge(sendMessageRequest)) {
      sendMessageRequest = storeMessageInS3(sendMessageRequest);
    }

    return super.sendMessageAsync(sendMessageRequest, asyncHandler);
  }

  public Future<SendMessageResult> sendMessageAsync(String queueUrl, String messageBody) {
    return sendMessageAsync(new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(messageBody));
  }

  public Future<SendMessageResult> sendMessageAsync(String queueUrl, String messageBody,
      AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler) {
    return sendMessageAsync(new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(messageBody), asyncHandler);
  }

  public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest) {
    return deleteMessageAsync(deleteMessageRequest, null);
  }

  public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest,
      AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler) {

    if (deleteMessageRequest == null) {
      String errorMessage = "deleteMessageRequest cannot be null.";
      LOG.error(errorMessage);
      throw new AmazonClientException(errorMessage);
    }

    deleteMessageRequest.getRequestClientOptions().appendUserAgent(SQSExtendedClientConstants.USER_AGENT_HEADER);

    if (!clientConfiguration.isLargePayloadSupportEnabled()) {
      return super.deleteMessageAsync(deleteMessageRequest, asyncHandler);
    }

    String receiptHandle = deleteMessageRequest.getReceiptHandle();
    String origReceiptHandle = receiptHandle;
    if (isS3ReceiptHandle(receiptHandle)) {
      deleteMessagePayloadFromS3(receiptHandle);
      origReceiptHandle = getOrigReceiptHandle(receiptHandle);
    }
    deleteMessageRequest.setReceiptHandle(origReceiptHandle);
    return super.deleteMessageAsync(deleteMessageRequest, asyncHandler);
  }

  public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl, String receiptHandle) {
    return deleteMessageAsync(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(receiptHandle));
  }

  public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl, String receiptHandle,
      AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler) {
    return deleteMessageAsync(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(receiptHandle),
        asyncHandler);
  }

}
