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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the AmazonSQSExtendedClient class.
 */
public class AmazonSQSExtendedClientTest {

    private AmazonSQS extendedSqsWithDefaultConfig;
    private AmazonSQS mockSqsBackend;
    private AmazonS3 mockS3;
    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String SQS_QUEUE_URL = "test-queue-url";

    private static final int LESS_THAN_SQS_SIZE_LIMIT = 3;
    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;

    // should be > 1 and << SQS_SIZE_LIMIT
    private static final int ARBITRARY_SMALLER_THRESHOLD = 500;

    @Before
    public void setupClient() {
        mockS3 = mock(AmazonS3.class);
        mockSqsBackend = mock(AmazonSQS.class);
        when(mockS3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

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

        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class));
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
        Assert.assertEquals(expectedRequest, messageRequest);

        sqsExtended.receiveMessage(messageRequest);
        Assert.assertEquals(expectedRequest, messageRequest);
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

        List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<SendMessageBatchRequestEntry>();
        for (int i = 0; i < 10; i++) {
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            int messageLength = messageLengthForCounter[i];
            String messageBody = generateStringWithLength(messageLength);
            entry.setMessageBody(messageBody);
            entry.setId("entry_" + i);
            batchEntries.add(entry);
        }

        SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(SQS_QUEUE_URL, batchEntries);
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

        List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<SendMessageBatchRequestEntry>();
        for (int i = 0; i < 10; i++) {
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            int messageLength = messageLengthForCounter[i];
            String messageBody = generateStringWithLength(messageLength);
            entry.setMessageBody(messageBody);
            entry.setId("entry_" + i);
            batchEntries.add(entry);
        }

        SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(SQS_QUEUE_URL, batchEntries);
        extendedSqsWithDefaultConfig.sendMessageBatch(batchRequest);

        // There should be 8 puts for the 8 messages above the threshold
        verify(mockS3, times(8)).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testWhenMessageBatchIsSentWhereSumOfMessageSizesIsOverTheThresholdThenAllArePutToS3() {
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

        List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<SendMessageBatchRequestEntry>();
        for (int i = 0; i < 10; i++) {
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            int messageLength = messageLengthForCounter[i];
            String messageBody = generateStringWithLength(messageLength);
            entry.setMessageBody(messageBody);
            entry.setId("entry_" + i);
            batchEntries.add(entry);
        }

        SendMessageBatchRequest batchRequest = new SendMessageBatchRequest(SQS_QUEUE_URL, batchEntries);
        extendedSqsWithDefaultConfig.sendMessageBatch(batchRequest);

        // There should be 10 puts, as the whole batch is moved to s3
        verify(mockS3, times(10)).putObject(isA(PutObjectRequest.class));
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
        Assert.assertTrue(attributes.isEmpty());
    }

    @Test
    public void testWhenLargeMessgaeIsSentThenAttributeWithPayloadSizeIsAdded() {
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().getMessageAttributes();
        Assert.assertEquals("Number", attributes.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME).getDataType());
        Assert.assertEquals(messageLength, (int)Integer.valueOf(attributes.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME).getStringValue()));
    }

    private String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }

}
