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

import static com.amazon.sqs.javamessaging.StringTestUtil.generateStringWithLength;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.payloadoffloading.PayloadS3Pointer;
import software.amazon.payloadoffloading.ServerSideEncryptionFactory;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClient.USER_AGENT_NAME;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedClient.USER_AGENT_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the AmazonSQSExtendedClient class.
 */
public class ThirdAmazonSQSExtendedClientTest {

    private SqsClient extendedSqsWithDefaultConfig;
    private SqsClient extendedSqsWithCustomKMS;
    private SqsClient extendedSqsWithDefaultKMS;
    private SqsClient extendedSqsWithGenericReservedAttributeName;
    private SqsClient extendedSqsWithDeprecatedMethods;
    private SqsClient extendedSqsWithS3KeyPrefix;
    private SqsClient mockSqsBackend;
    private S3Client mockS3;

    private MockedStatic<UUID> uuidMockStatic;
    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String SQS_QUEUE_URL = "test-queue-url";
    private static final String S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID = "test-customer-managed-kms-key-id";
    private static final String S3_KEY_PREFIX = "test-s3-key-prefix";
    private static final String S3_KEY_UUID = "test-s3-key-uuid";

    private static final int LESS_THAN_SQS_SIZE_LIMIT = 3;
    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;
    private static final ServerSideEncryptionStrategy SERVER_SIDE_ENCRYPTION_CUSTOM_STRATEGY =
            ServerSideEncryptionFactory.customerKey(S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID);
    private static final ServerSideEncryptionStrategy SERVER_SIDE_ENCRYPTION_DEFAULT_STRATEGY =
            ServerSideEncryptionFactory.awsManagedCmk();

    // should be > 1 and << SQS_SIZE_LIMIT
    private static final int ARBITRARY_SMALLER_THRESHOLD = 500;

    @BeforeEach
    public void setupClients() {
        uuidMockStatic = mockStatic(UUID.class);
        mockS3 = mock(S3Client.class);
        mockSqsBackend = mock(SqsClient.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME);

        ExtendedClientConfiguration extendedClientConfigurationWithCustomKMS = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withServerSideEncryption(SERVER_SIDE_ENCRYPTION_CUSTOM_STRATEGY);

        ExtendedClientConfiguration extendedClientConfigurationWithDefaultKMS = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withServerSideEncryption(SERVER_SIDE_ENCRYPTION_DEFAULT_STRATEGY);

        ExtendedClientConfiguration extendedClientConfigurationWithGenericReservedAttributeName = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withLegacyReservedAttributeNameDisabled();

        ExtendedClientConfiguration extendedClientConfigurationDeprecated = new ExtendedClientConfiguration().withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME);

        ExtendedClientConfiguration extendedClientConfigurationWithS3KeyPrefix = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withS3KeyPrefix(S3_KEY_PREFIX);

        UUID uuidMock = mock(UUID.class);
        when(uuidMock.toString()).thenReturn(S3_KEY_UUID);
        uuidMockStatic.when(UUID::randomUUID).thenReturn(uuidMock);
        extendedSqsWithDefaultConfig = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));
        extendedSqsWithCustomKMS = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationWithCustomKMS));
        extendedSqsWithDefaultKMS = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationWithDefaultKMS));
        extendedSqsWithGenericReservedAttributeName = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationWithGenericReservedAttributeName));
        extendedSqsWithDeprecatedMethods = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationDeprecated));
        extendedSqsWithS3KeyPrefix = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfigurationWithS3KeyPrefix));
    }

    @AfterEach
    public void tearDown() {
        uuidMockStatic.close();
    }

    @Test
    public void testDefaultExtendedClientDeletesSmallMessage() {
        // given
        String receiptHandle = UUID.randomUUID().toString();
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder().queueUrl(SQS_QUEUE_URL).receiptHandle(receiptHandle).build();

        // when
        extendedSqsWithDefaultConfig.deleteMessage(deleteRequest);

        // then
        ArgumentCaptor<DeleteMessageRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(mockSqsBackend).deleteMessage(deleteRequestCaptor.capture());
        assertEquals(receiptHandle, deleteRequestCaptor.getValue().receiptHandle());
        verifyNoInteractions(mockS3);
    }

    @Test
    public void testDefaultExtendedClientDeletesObjectS3UponMessageDelete() {
        // given
        String randomS3Key = UUID.randomUUID().toString();
        String originalReceiptHandle = UUID.randomUUID().toString();
        String largeMessageReceiptHandle = getLargeReceiptHandle(randomS3Key, originalReceiptHandle);
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder().queueUrl(SQS_QUEUE_URL).receiptHandle(largeMessageReceiptHandle).build();

        // when
        extendedSqsWithDefaultConfig.deleteMessage(deleteRequest);

        // then
        ArgumentCaptor<DeleteMessageRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(mockSqsBackend).deleteMessage(deleteRequestCaptor.capture());
        assertEquals(originalReceiptHandle, deleteRequestCaptor.getValue().receiptHandle());
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(S3_BUCKET_NAME).key(randomS3Key).build();
        verify(mockS3).deleteObject(eq(deleteObjectRequest));
    }

    @Test
    public void testExtendedClientConfiguredDoesNotDeleteObjectFromS3UponDelete() {
        // given
        String randomS3Key = UUID.randomUUID().toString();
        String originalReceiptHandle = UUID.randomUUID().toString();
        String largeMessageReceiptHandle = getLargeReceiptHandle(randomS3Key, originalReceiptHandle);
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder().queueUrl(SQS_QUEUE_URL).receiptHandle(largeMessageReceiptHandle).build();

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME, false);

        SqsClient extendedSqs = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));

        // when
        extendedSqs.deleteMessage(deleteRequest);

        // then
        ArgumentCaptor<DeleteMessageRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(mockSqsBackend).deleteMessage(deleteRequestCaptor.capture());
        assertEquals(originalReceiptHandle, deleteRequestCaptor.getValue().receiptHandle());
        verifyNoInteractions(mockS3);
    }

    @Test
    public void testExtendedClientConfiguredDoesNotDeletesObjectsFromS3UponDeleteBatch() {
        // given
        int batchSize = 10;
        List<String> originalReceiptHandles = IntStream.range(0, batchSize)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        DeleteMessageBatchRequest deleteBatchRequest = generateLargeDeleteBatchRequest(originalReceiptHandles);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME, false);
        SqsClient extendedSqs = spy(new AmazonSQSExtendedClient(mockSqsBackend, extendedClientConfiguration));

        // when
        extendedSqs.deleteMessageBatch(deleteBatchRequest);

        // then
        ArgumentCaptor<DeleteMessageBatchRequest> deleteBatchRequestCaptor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(mockSqsBackend, times(1)).deleteMessageBatch(deleteBatchRequestCaptor.capture());
        DeleteMessageBatchRequest request = deleteBatchRequestCaptor.getValue();
        assertEquals(originalReceiptHandles.size(), request.entries().size());
        IntStream.range(0, originalReceiptHandles.size()).forEach(i -> assertEquals(
                originalReceiptHandles.get(i),
                request.entries().get(i).receiptHandle()));
        verifyNoInteractions(mockS3);
    }

    @Test
    public void testDefaultExtendedClientDeletesObjectsFromS3UponDeleteBatch() {
        // given
        int batchSize = 10;
        List<String> originalReceiptHandles = IntStream.range(0, batchSize)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        DeleteMessageBatchRequest deleteBatchRequest = generateLargeDeleteBatchRequest(originalReceiptHandles);

        // when
        extendedSqsWithDefaultConfig.deleteMessageBatch(deleteBatchRequest);

        // then
        ArgumentCaptor<DeleteMessageBatchRequest> deleteBatchRequestCaptor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(mockSqsBackend, times(1)).deleteMessageBatch(deleteBatchRequestCaptor.capture());
        DeleteMessageBatchRequest request = deleteBatchRequestCaptor.getValue();
        assertEquals(originalReceiptHandles.size(), request.entries().size());
        IntStream.range(0, originalReceiptHandles.size()).forEach(i -> assertEquals(
                originalReceiptHandles.get(i),
                request.entries().get(i).receiptHandle()));
        verify(mockS3, times(batchSize)).deleteObject(any(DeleteObjectRequest.class));
    }

    private void testReceiveMessage_when_MessageIsLarge(String reservedAttributeName) {
        String pointer = new PayloadS3Pointer(S3_BUCKET_NAME, "S3Key").toJson();
        Message message = Message.builder()
                .messageAttributes(ImmutableMap.of(reservedAttributeName, MessageAttributeValue.builder().build()))
                .body(pointer)
                .build();
        String expectedMessage = "LargeMessage";
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key("S3Key")
                .build();

        ResponseInputStream<GetObjectResponse> s3Object = new ResponseInputStream<>(GetObjectResponse.builder().build(), AbortableInputStream.create(new StringInputStream(expectedMessage)));
//        S3Object s3Object = S3Object.builder().build();
//        s3Object.setObjectContent(new StringInputStream(expectedMessage));
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(
                ReceiveMessageResponse.builder().messages(message).build());
        when(mockS3.getObject(isA(GetObjectRequest.class))).thenReturn(s3Object);

        ReceiveMessageRequest messageRequest = ReceiveMessageRequest.builder().build();
        ReceiveMessageResponse actualReceiveMessageResponse = extendedSqsWithDefaultConfig.receiveMessage(messageRequest);
        Message actualMessage = actualReceiveMessageResponse.messages().get(0);

        assertEquals(expectedMessage, actualMessage.body());
        assertFalse(actualMessage.messageAttributes().keySet().containsAll(AmazonSQSExtendedClient.RESERVED_ATTRIBUTE_NAMES));
        verify(mockS3, times(1)).getObject(isA(GetObjectRequest.class));
    }

    private DeleteMessageBatchRequest generateLargeDeleteBatchRequest(List<String> originalReceiptHandles) {
        List<DeleteMessageBatchRequestEntry> deleteEntries = IntStream.range(0, originalReceiptHandles.size())
                .mapToObj(i -> DeleteMessageBatchRequestEntry.builder()
                    .id(Integer.toString(i))
                    .receiptHandle(getSampleLargeReceiptHandle(originalReceiptHandles.get(i)))
                    .build())
                .collect(Collectors.toList());

        return DeleteMessageBatchRequest.builder().queueUrl(SQS_QUEUE_URL).entries(deleteEntries).build();
    }

    private String getLargeReceiptHandle(String s3Key, String originalReceiptHandle) {
        return SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + S3_BUCKET_NAME
                + SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + SQSExtendedClientConstants.S3_KEY_MARKER
                + s3Key + SQSExtendedClientConstants.S3_KEY_MARKER + originalReceiptHandle;
    }

    private String getSampleLargeReceiptHandle(String originalReceiptHandle) {
        return getLargeReceiptHandle(UUID.randomUUID().toString(), originalReceiptHandle);
    }
}
