package com.amazon.sqs.javamessaging;

import java.util.List;

/**
 * Response containing messages with streaming access to large payloads.
 */
public class ReceiveStreamMessageResponse {
    private final List<StreamMessage> streamMessages;

    public ReceiveStreamMessageResponse(List<StreamMessage> streamMessages) {
        this.streamMessages = streamMessages;
    }

    /**
     * @return list of messages with streaming payload access
     */
    public List<StreamMessage> streamMessages() {
        return streamMessages;
    }
}