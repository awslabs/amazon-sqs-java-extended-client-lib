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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the AmazonSQSExtendedClient class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AmazonSQSExtendedClientTest {

    private AmazonSQS extendedSqsWithDefaultConfig;

    @Mock
    private AmazonSQS mockSqsBackend;

    @Mock
    private AmazonS3 mockS3;

    private List<List<SendMessageBatchRequestEntry>> sendMessageBatchInvocationEntries = new ArrayList<>();

    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String SQS_QUEUE_URL = "test-queue-url";

    private static final int LESS_THAN_SQS_SIZE_LIMIT = 3;
    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;

    // should be > 1 and << SQS_SIZE_LIMIT
    private static final int ARBITRARY_SMALLER_THRESHOLD = 500;

    @Before
    public void setupClient() {

        when(mockS3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        // send message batch must return a result and must record the entries used
        // we can't use a captor for this as the send message batch request object is
        // changed during execution, so the captor ends up with multiple references to
        // the same object and we don't see the state of the earliest
        when(mockSqsBackend.sendMessageBatch(any(SendMessageBatchRequest.class)))
                .thenAnswer(new Answer<SendMessageBatchResult>() {

                    @Override
                    public SendMessageBatchResult answer(InvocationOnMock invocation) throws Throwable {
                        // record the entries
                        List<SendMessageBatchRequestEntry> entries =
                                invocation.getArgumentAt(0, SendMessageBatchRequest.class).getEntries();
                        sendMessageBatchInvocationEntries.add(entries);

                        return new SendMessageBatchResult();
                    }
                });

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(mockS3, S3_BUCKET_NAME);

        extendedSqsWithDefaultConfig = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));

    }

    @Test
    public void testWhenSendLargeMessageThenPayloadIsStoredInS3() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testWhenSendSmallMessageThenS3IsNotUsed() {
        int messageLength = SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        verify(mockS3, never()).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testWhenSendMessageWithLargePayloadSupportDisabledThenS3IsNotUsedAndSqsBackendIsResponsibleToFailIt() {
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportDisabled();
        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);

        verify(mockS3, never()).putObject(isA(PutObjectRequest.class));
        verify(mockSqsBackend).sendMessage(eq(messageRequest));
    }

    @Test
    public void testWhenSendMessageWithAlwaysThroughS3AndMessageIsSmallThenItIsStillStoredInS3() {
        int messageLength = LESS_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withAlwaysThroughS3(true);
        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);

        verify(mockS3).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testWhenSendMessageWithSetMessageSizeThresholdThenThresholdIsHonored() {
        int messageLength = ARBITRARY_SMALLER_THRESHOLD * 2;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withMessageSizeThreshold(ARBITRARY_SMALLER_THRESHOLD);

        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);
        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testReceiveMessageMultipleTimesDoesNotAdditionallyAlterReceiveMessageRequest() {
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(mockS3, S3_BUCKET_NAME);
        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult());

        ReceiveMessageRequest messageRequest = new ReceiveMessageRequest();
        ReceiveMessageRequest expectedRequest = new ReceiveMessageRequest()
                .withMessageAttributeNames(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);

        sqsExtended.receiveMessage(messageRequest);
        assertEquals(expectedRequest, messageRequest);

        sqsExtended.receiveMessage(messageRequest);
        assertEquals(expectedRequest, messageRequest);
    }

    @Test
    public void testWhenSmallMessageBatchIsSentThenNoMessagesStoredInS3() {
        // This creates 10 messages all well within the threshold

        int[] messageLengthForCounter = new int[] {
                1_000,
                1_000,
                1_000,
                1_000,
                1_000,
                1_000,
                1_000,
                1_000,
                1_000,
                1_000
        };

        SendMessageBatchRequest batchRequest = createMessageBatchWithSizes(messageLengthForCounter);
        extendedSqsWithDefaultConfig.sendMessageBatch(batchRequest);

        // There should be no puts
        verify(mockS3, never()).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testWhenMessageBatchIsSentThenOnlyMessagesLargerThanThresholdAreStoredInS3() {
        // This creates 10 messages, out of which only two are below the threshold (100K and 20K),
        // and the other 8 are above the threshold

        int[] messageLengthForCounter = new int[] {
                100_000,
                300_000,
                400_000,
                500_000,
                600_000,
                700_000,
                800_000,
                900_000,
                20_000,
                1000_000
        };

        SendMessageBatchRequest batchRequest = createMessageBatchWithSizes(messageLengthForCounter);
        extendedSqsWithDefaultConfig.sendMessageBatch(batchRequest);

        // There should be 8 puts for the 8 messages above the threshold
        verify(mockS3, times(8)).putObject(isA(PutObjectRequest.class));

        // and one batch send
        verify(mockSqsBackend).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    public void testWhenMessageBatchIsSentWhereSumOfMessageSizesIsOverTheThresholdThenBatchIsSplit() {
        // This creates 10 messages, all of which are below the threshold, but together would make
        // a single request over the threshold

        int[] messageLengthForCounter = new int[] {
                26_214,
                26_214,
                26_214,
                26_214,
                26_214,
                26_214,
                26_214,
                26_214,
                26_214,
                26_219
        };

        SendMessageBatchRequest batchRequest = createMessageBatchWithSizes(messageLengthForCounter);
        extendedSqsWithDefaultConfig.sendMessageBatch(batchRequest);

        // The client should not put any objects to S3
        verify(mockS3, never()).putObject(isA(PutObjectRequest.class));

        // The client should have made two requests to SQS
        verify(mockSqsBackend, times(2)).sendMessageBatch(any(SendMessageBatchRequest.class));

        // the client will have put most messages in the first batch, then the remainder in a second
        assertEquals(9, sendMessageBatchInvocationEntries.get(0).size());
        assertEquals(1, sendMessageBatchInvocationEntries.get(1).size());
    }

    @Test
    public void testWhenMessageBatchIsMadeOfLargeMessagesThenBatchIsSplitAndOrderMaintained() {
        // This creates 10 messages, all of which are below the threshold, but together would make
        // a single request over the threshold

        int[] messageLengthForCounter = new int[] {
                SQS_SIZE_LIMIT,
                SQS_SIZE_LIMIT - 1,
                SQS_SIZE_LIMIT - 2,
                SQS_SIZE_LIMIT - 3,
                SQS_SIZE_LIMIT - 4,
                SQS_SIZE_LIMIT - 5,
                SQS_SIZE_LIMIT - 6,
                SQS_SIZE_LIMIT - 7,
                SQS_SIZE_LIMIT - 8,
                SQS_SIZE_LIMIT - 9
        };

        SendMessageBatchRequest batchRequest = createMessageBatchWithSizes(messageLengthForCounter);
        extendedSqsWithDefaultConfig.sendMessageBatch(batchRequest);

        // The client should not put any objects to S3 as they are all small enough
        // to send to SQS
        verify(mockS3, never()).putObject(isA(PutObjectRequest.class));

        // The client should have sent each item as a batch request
        verify(mockSqsBackend, times(10)).sendMessageBatch(any(SendMessageBatchRequest.class));

        // the order of messages has been preserved
        for (int i = 0; i < messageLengthForCounter.length; i++) {
            // each batch should correspond to the message length from the list
            assertEquals(messageLengthForCounter[i],
                    sendMessageBatchInvocationEntries.get(i).get(0).getMessageBody().length());
        }
    }

    @Test
    public void testWhenSmallMessageIsSentThenNoAttributeIsAdded() {
        int messageLength = LESS_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().getMessageAttributes();
        assertTrue(attributes.isEmpty());
    }

    @Test
    public void testWhenLargeMessageIsSentThenAttributeWithPayloadSizeIsAdded() {
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().getMessageAttributes();
        assertEquals("Number", attributes.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME).getDataType());
        assertEquals(messageLength, (int)Integer.valueOf(attributes.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME).getStringValue()));
    }

    private SendMessageBatchRequest createMessageBatchWithSizes(int[] messageLengthForCounter) {
        List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            int messageLength = messageLengthForCounter[i];
            String messageBody = generateStringWithLength(messageLength);
            entry.setMessageBody(messageBody);
            entry.setId("entry_" + i);
            batchEntries.add(entry);
        }

        return new SendMessageBatchRequest(SQS_QUEUE_URL, batchEntries);
    }

    private String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }

}
