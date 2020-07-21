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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;

import com.amazonaws.util.StringInputStream;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.payloadoffloading.PayloadS3Pointer;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the AmazonSQSExtendedClient class.
 */
public class AmazonSQSExtendedClientTest {

    private AmazonSQS extendedSqsWithDefaultConfig;
    private AmazonSQS extendedSqsWithCustomKMS;
    private AmazonSQS extendedSqsWithDefaultKMS;
    private AmazonSQS extendedSqsWithGenericReservedAttributeName;
    private AmazonSQS mockSqsBackend;
    private AmazonS3 mockS3;
    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String SQS_QUEUE_URL = "test-queue-url";
    private static final String S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID = "test-customer-managed-kms-key-id";

    private static final int LESS_THAN_SQS_SIZE_LIMIT = 3;
    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;

    // should be > 1 and << SQS_SIZE_LIMIT
    private static final int ARBITRATY_SMALLER_THRESSHOLD = 500;

    @Before
    public void setupClients() {
        mockS3 = mock(AmazonS3.class);
        mockSqsBackend = mock(AmazonSQS.class);
        when(mockS3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME);

        ExtendedClientConfiguration extendedClientConfigurationWithCustomKMS = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID));

        ExtendedClientConfiguration extendedClientConfigurationWithDefaultKMS = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams());

        ExtendedClientConfiguration extendedClientConfigurationWithGenericReservedAttributeName = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withLegacyReservedAttributeNameDisabled();

        extendedSqsWithDefaultConfig = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));
        extendedSqsWithCustomKMS = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationWithCustomKMS));
        extendedSqsWithDefaultKMS = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationWithDefaultKMS));
        extendedSqsWithGenericReservedAttributeName = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationWithGenericReservedAttributeName));
    }

    @Test
    public void testWhenSendLargeMessageThenPayloadIsStoredInS3() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testWhenSendLargeMessage_WithoutKMS_ThenPayloadIsStoredInS3AndKMSKeyIdIsNotUsed() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3, times(1)).putObject(putObjectRequestArgumentCaptor.capture());

        Assert.assertTrue(putObjectRequestArgumentCaptor.getValue().getSSEAwsKeyManagementParams() == null);
        Assert.assertTrue(putObjectRequestArgumentCaptor.getValue().getBucketName().equals(S3_BUCKET_NAME));
    }

    @Test
    public void testWhenSendLargeMessage_WithCustomKMS_ThenPayloadIsStoredInS3AndCorrectKMSKeyIdIsNotUsed() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithCustomKMS.sendMessage(messageRequest);

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3, times(1)).putObject(putObjectRequestArgumentCaptor.capture());

        Assert.assertTrue(putObjectRequestArgumentCaptor.getValue().getSSEAwsKeyManagementParams()
                .getAwsKmsKeyId().equals(S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID));
        Assert.assertTrue(putObjectRequestArgumentCaptor.getValue().getBucketName().equals(S3_BUCKET_NAME));
    }

    @Test
    public void testWhenSendLargeMessage_WithDefaultKMS_ThenPayloadIsStoredInS3AndCorrectKMSKeyIdIsNotUsed() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultKMS.sendMessage(messageRequest);

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3, times(1)).putObject(putObjectRequestArgumentCaptor.capture());

        Assert.assertTrue(putObjectRequestArgumentCaptor.getValue().getSSEAwsKeyManagementParams() != null &&
                putObjectRequestArgumentCaptor.getValue().getSSEAwsKeyManagementParams().getAwsKmsKeyId() == null);
        Assert.assertTrue(putObjectRequestArgumentCaptor.getValue().getBucketName().equals(S3_BUCKET_NAME));
    }

    @Test
    public void testSendLargeMessageWithDefaultConfigThenLegacyReservedAttributeNameIsUsed(){
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);
        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().getMessageAttributes();
        Assert.assertTrue(attributes.keySet().contains(AmazonSQSExtendedClient.LEGACY_RESERVED_ATTRIBUTE_NAME));
        Assert.assertFalse(attributes.keySet().contains(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME));

    }

    @Test
    public void testSendLargeMessageWithGenericReservedAttributeNameConfigThenGenericReservedAttributeNameIsUsed(){
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);
        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithGenericReservedAttributeName.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().getMessageAttributes();
        Assert.assertTrue(attributes.keySet().contains(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME));
        Assert.assertFalse(attributes.keySet().contains(AmazonSQSExtendedClient.LEGACY_RESERVED_ATTRIBUTE_NAME));
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
                .withPayloadSupportDisabled();
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
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withAlwaysThroughS3(true);
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
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withPayloadSizeThreshold(ARBITRATY_SMALLER_THRESSHOLD);

        AmazonSQS sqsExtended = spy(new AmazonSQSExtendedClient(mock(AmazonSQSClient.class), extendedClientConfiguration));

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        sqsExtended.sendMessage(messageRequest);
        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testReceiveMessageMultipleTimesDoesNotAdditionallyAlterReceiveMessageRequest() {
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult());

        ReceiveMessageRequest messageRequest = new ReceiveMessageRequest();

        ReceiveMessageRequest expectedRequest = new ReceiveMessageRequest()
                .withMessageAttributeNames(AmazonSQSExtendedClient.RESERVED_ATTRIBUTE_NAMES);

        extendedSqsWithDefaultConfig.receiveMessage(messageRequest);
        Assert.assertEquals(expectedRequest, messageRequest);

        extendedSqsWithDefaultConfig.receiveMessage(messageRequest);
        Assert.assertEquals(expectedRequest, messageRequest);
    }

    @Test
    public void testReceiveMessage_when_MessageIsLarge_legacyReservedAttributeUsed() throws Exception {
        testReceiveMessage_when_MessageIsLarge(AmazonSQSExtendedClient.LEGACY_RESERVED_ATTRIBUTE_NAME);
    }

    @Test
    public void testReceiveMessage_when_MessageIsLarge_ReservedAttributeUsed() throws Exception {
        testReceiveMessage_when_MessageIsLarge(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
    }

    @Test
    public void testReceiveMessage_when_MessageIsSmall() throws Exception {
        String expectedMessageAttributeName = "AnyMessageAttribute";
        String expectedMessage = "SmallMessage";
        Message message = new Message().addMessageAttributesEntry(expectedMessageAttributeName, mock(MessageAttributeValue.class));
        message.setBody(expectedMessage);
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult().withMessages(message));

        ReceiveMessageRequest messageRequest = new ReceiveMessageRequest();
        ReceiveMessageResult actualReceiveMessageResult = extendedSqsWithDefaultConfig.receiveMessage(messageRequest);
        Message actualMessage = actualReceiveMessageResult.getMessages().get(0);

        Assert.assertEquals(expectedMessage, actualMessage.getBody());
        Assert.assertTrue(actualMessage.getMessageAttributes().keySet().contains(expectedMessageAttributeName));
        Assert.assertFalse(actualMessage.getMessageAttributes().keySet().containsAll(AmazonSQSExtendedClient.RESERVED_ATTRIBUTE_NAMES));
        verifyZeroInteractions(mockS3);
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
    public void testWhenLargeMessageIsSentThenAttributeWithPayloadSizeIsAdded() {
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);

        SendMessageRequest messageRequest = new SendMessageRequest(SQS_QUEUE_URL, messageBody);
        extendedSqsWithDefaultConfig.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().getMessageAttributes();
        Assert.assertEquals("Number", attributes.get(AmazonSQSExtendedClient.LEGACY_RESERVED_ATTRIBUTE_NAME).getDataType());
        Assert.assertEquals(messageLength, (int) Integer.valueOf(attributes.get(AmazonSQSExtendedClient.LEGACY_RESERVED_ATTRIBUTE_NAME).getStringValue()));
    }

    @Test
    public void testDefaultExtendedClientDeletesSmallMessage() {
        // given
        String receiptHandle = UUID.randomUUID().toString();
        DeleteMessageRequest deleteRequest = new DeleteMessageRequest(SQS_QUEUE_URL, receiptHandle);

        // when
        extendedSqsWithDefaultConfig.deleteMessage(deleteRequest);

        // then
        ArgumentCaptor<DeleteMessageRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(mockSqsBackend).deleteMessage(deleteRequestCaptor.capture());
        Assert.assertEquals(receiptHandle, deleteRequestCaptor.getValue().getReceiptHandle());
        verifyZeroInteractions(mockS3);
    }

    @Test
    public void testDefaultExtendedClientDeletesObjectS3UponMessageDelete() {
        // given
        String randomS3Key = UUID.randomUUID().toString();
        String originalReceiptHandle = UUID.randomUUID().toString();
        String largeMessageReceiptHandle = getLargeReceiptHandle(randomS3Key, originalReceiptHandle);
        DeleteMessageRequest deleteRequest = new DeleteMessageRequest(SQS_QUEUE_URL, largeMessageReceiptHandle);

        // when
        extendedSqsWithDefaultConfig.deleteMessage(deleteRequest);

        // then
        ArgumentCaptor<DeleteMessageRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(mockSqsBackend).deleteMessage(deleteRequestCaptor.capture());
        Assert.assertEquals(originalReceiptHandle, deleteRequestCaptor.getValue().getReceiptHandle());
        verify(mockS3).deleteObject(eq(S3_BUCKET_NAME), eq(randomS3Key));
    }

    @Test
    public void testExtendedClientConfiguredDoesNotDeleteObjectFromS3UponDelete() {
        // given
        String randomS3Key = UUID.randomUUID().toString();
        String originalReceiptHandle = UUID.randomUUID().toString();
        String largeMessageReceiptHandle = getLargeReceiptHandle(randomS3Key, originalReceiptHandle);
        DeleteMessageRequest deleteRequest = new DeleteMessageRequest(SQS_QUEUE_URL, largeMessageReceiptHandle);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME, false);

        AmazonSQS extendedSqs = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));

        // when
        extendedSqs.deleteMessage(deleteRequest);

        // then
        ArgumentCaptor<DeleteMessageRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(mockSqsBackend).deleteMessage(deleteRequestCaptor.capture());
        Assert.assertEquals(originalReceiptHandle, deleteRequestCaptor.getValue().getReceiptHandle());
        verifyZeroInteractions(mockS3);
    }

    @Test
    public void testExtendedClientConfiguredDoesNotDeletesObjectsFromS3UponDeleteBatch() {
        // given
        int batchSize = 10;
        DeleteMessageBatchRequest deleteBatchRequest = generateLargeDeleteBatchRequest(batchSize);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME, false);
        AmazonSQS extendedSqs = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));

        // when
        extendedSqs.deleteMessageBatch(deleteBatchRequest);

        // then
        verify(mockSqsBackend, times(1)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
        verifyZeroInteractions(mockS3);
    }

    @Test
    public void testDefaultExtendedClientDeletesObjectsFromS3UponDeleteBatch() {
        // given
        int batchSize = 10;
        DeleteMessageBatchRequest deleteBatchRequest = generateLargeDeleteBatchRequest(batchSize);

        // when
        extendedSqsWithDefaultConfig.deleteMessageBatch(deleteBatchRequest);

        // then
        verify(mockSqsBackend, times(1)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
        verify(mockS3, times(batchSize)).deleteObject(eq(S3_BUCKET_NAME), anyString());
    }

    private void testReceiveMessage_when_MessageIsLarge(String reservedAttributeName) throws Exception {
        Message message = new Message().addMessageAttributesEntry(reservedAttributeName, mock(MessageAttributeValue.class));
        String pointer = new PayloadS3Pointer(S3_BUCKET_NAME, "S3Key").toJson();
        message.setBody(pointer);
        String expectedMessage = "LargeMessage";
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream(expectedMessage));
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult().withMessages(message));
        when(mockS3.getObject(isA(GetObjectRequest.class))).thenReturn(s3Object);

        ReceiveMessageRequest messageRequest = new ReceiveMessageRequest();
        ReceiveMessageResult actualReceiveMessageResult = extendedSqsWithDefaultConfig.receiveMessage(messageRequest);
        Message actualMessage = actualReceiveMessageResult.getMessages().get(0);

        Assert.assertEquals(expectedMessage, actualMessage.getBody());
        Assert.assertFalse(actualMessage.getMessageAttributes().keySet().containsAll(AmazonSQSExtendedClient.RESERVED_ATTRIBUTE_NAMES));
        verify(mockS3, times(1)).getObject(isA(GetObjectRequest.class));
    }

    private DeleteMessageBatchRequest generateLargeDeleteBatchRequest(int size) {
        List<DeleteMessageBatchRequestEntry> deleteEntries = IntStream.range(0, size)
                .mapToObj(i -> new DeleteMessageBatchRequestEntry(Integer.toString(i), getSampleLargeReceiptHandle()))
                .collect(Collectors.toList());

        return new DeleteMessageBatchRequest(SQS_QUEUE_URL, deleteEntries);
    }

    private String getLargeReceiptHandle(String s3Key, String originalReceiptHandle) {
        return SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + S3_BUCKET_NAME
                + SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + SQSExtendedClientConstants.S3_KEY_MARKER
                + s3Key + SQSExtendedClientConstants.S3_KEY_MARKER + originalReceiptHandle;
    }

    private String getSampleLargeReceiptHandle() {
        return getLargeReceiptHandle(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }

}
