package com.amazon.sqs.javamessaging;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Represents a message received from SQS that can provide streaming access to large payloads stored in S3.
 * Instead of loading the entire payload into memory, this provides access to the payload as a stream.
 */
public class StreamMessage {
    private final Message originalMessage;
    private final ResponseInputStream<GetObjectResponse> payloadStream;

    public StreamMessage(Message originalMessage, ResponseInputStream<GetObjectResponse> payloadStream) {
        this.originalMessage = originalMessage;
        this.payloadStream = payloadStream;
    }

    /**
     * @return the original SQS message metadata (messageId, receiptHandle, attributes, etc.)
     */
    public Message getMessage() {
        return originalMessage;
    }

    /**
     * @return stream for accessing the message payload, or null if payload is not stored in S3
     */
    public ResponseInputStream<GetObjectResponse> getPayloadStream() {
        return payloadStream;
    }

    /**
     * @return true if this message has a streaming payload from S3
     */
    public boolean hasStreamPayload() {
        return payloadStream != null;
    }
}