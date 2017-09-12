/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the AmazonSQSExtendedClient class.
 */
public class AmazonSQSExtendedClientTest {

    private AmazonSQS extendedSqsWithDefaultConfig;
    private AmazonSQS mockSqsBackend;
    private AmazonS3 mockS3;
    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String S3_PREFIX = "sub/folder/";
    private static final String SQS_QUEUE_URL = "test-queue-url";

    private static final int LESS_THAN_SQS_SIZE_LIMIT = 3;
    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;

    // should be > 1 and << SQS_SIZE_LIMIT
    private static final int ARBITRATY_SMALLER_THRESSHOLD = 500;

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
        int messageLength = ARBITRATY_SMALLER_THRESSHOLD * 2;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withMessageSizeThreshold(ARBITRATY_SMALLER_THRESSHOLD);

        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);
        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testWhenMessageBatchIsSentThenOnlyMessagesLargerThanThresholdAreStoredInS3() {
        // This creates 10 messages, out of which only two are below the threshold (100K and 200K),
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
                200_000,
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

        // There should be 8 puts for the 8 messages above the threshhold
        verify(mockS3, times(8)).putObject(isA(PutObjectRequest.class));
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

    @Test
    public void testWhenS3PrefixIsSetThenPrefixUsedWithS3Key() {
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);
        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(mockS3, S3_BUCKET_NAME, S3_PREFIX);

        AmazonSQS extendedSqs = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));
        extendedSqs.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().getMessageAttributes();
        Assert.assertEquals("Number", attributes.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME).getDataType());
        Assert.assertEquals(messageLength, (int)Integer.valueOf(attributes.get(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME).getStringValue()));
        Assert.assertTrue(getPointerFromJson(sendMessageRequestCaptor.getValue().getMessageBody()).getS3Key().startsWith(S3_PREFIX));
    }

    private String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }

    private MessageS3Pointer getPointerFromJson(String json) {
        JsonDataConverter jsonDataConverter = new JsonDataConverter();
        try {
            return jsonDataConverter.deserializeFromJson(json, MessageS3Pointer.class);
        } catch (Exception e) {
            Assert.fail();
        }
        return new MessageS3Pointer();
    }
}
