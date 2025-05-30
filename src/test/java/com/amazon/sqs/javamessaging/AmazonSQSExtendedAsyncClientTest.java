package com.amazon.sqs.javamessaging;

import static com.amazon.sqs.javamessaging.AmazonSQSExtendedAsyncClient.USER_AGENT_NAME;
import static com.amazon.sqs.javamessaging.AmazonSQSExtendedAsyncClient.USER_AGENT_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.payloadoffloading.PayloadS3Pointer;
import software.amazon.payloadoffloading.ServerSideEncryptionFactory;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

public class AmazonSQSExtendedAsyncClientTest {

    private SqsAsyncClient extendedSqsWithDefaultConfig;
    private SqsAsyncClient extendedSqsWithCustomKMS;
    private SqsAsyncClient extendedSqsWithDefaultKMS;
    private SqsAsyncClient extendedSqsWithGenericReservedAttributeName;
    private SqsAsyncClient mockSqsBackend;
    private S3AsyncClient mockS3;
    private static final String S3_BUCKET_NAME = "test-bucket-name";
    private static final String SQS_QUEUE_URL = "test-queue-url";
    private static final String S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID = "test-customer-managed-kms-key-id";

    private static final int LESS_THAN_SQS_SIZE_LIMIT = 3;
    private static final int SQS_SIZE_LIMIT = 262144;
    private static final int MORE_THAN_SQS_SIZE_LIMIT = SQS_SIZE_LIMIT + 1;
    private static final ServerSideEncryptionStrategy SERVER_SIDE_ENCRYPTION_CUSTOM_STRATEGY = ServerSideEncryptionFactory.customerKey(S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID);
    private static final ServerSideEncryptionStrategy SERVER_SIDE_ENCRYPTION_DEFAULT_STRATEGY = ServerSideEncryptionFactory.awsManagedCmk();

    // should be > 1 and << SQS_SIZE_LIMIT
    private static final int ARBITRARY_SMALLER_THRESHOLD = 500;

    @BeforeEach
    public void setupClients() {
        mockS3 = mock(S3AsyncClient.class);
        mockSqsBackend = mock(SqsAsyncClient.class);
        when(mockS3.putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class))).thenReturn(
            CompletableFuture.completedFuture(null));
        when(mockS3.deleteObject(isA(DeleteObjectRequest.class))).thenReturn(
            CompletableFuture.completedFuture(DeleteObjectResponse.builder().build()));
        when(mockSqsBackend.sendMessage(isA(SendMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(SendMessageResponse.builder().build()));
        when(mockSqsBackend.sendMessageBatch(isA(SendMessageBatchRequest.class))).thenReturn(
            CompletableFuture.completedFuture(SendMessageBatchResponse.builder().build()));
        when(mockSqsBackend.deleteMessage(isA(DeleteMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));
        when(mockSqsBackend.deleteMessageBatch(isA(DeleteMessageBatchRequest.class))).thenReturn(
            CompletableFuture.completedFuture(DeleteMessageBatchResponse.builder().build()));

        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME);

        ExtendedAsyncClientConfiguration extendedClientConfigurationWithCustomKMS = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
            .withServerSideEncryption(SERVER_SIDE_ENCRYPTION_CUSTOM_STRATEGY);

        ExtendedAsyncClientConfiguration extendedClientConfigurationWithDefaultKMS = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
            .withServerSideEncryption(SERVER_SIDE_ENCRYPTION_DEFAULT_STRATEGY);

        ExtendedAsyncClientConfiguration extendedClientConfigurationWithGenericReservedAttributeName = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withLegacyReservedAttributeNameDisabled();

        extendedSqsWithDefaultConfig = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));
        extendedSqsWithCustomKMS = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfigurationWithCustomKMS));
        extendedSqsWithDefaultKMS = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfigurationWithDefaultKMS));
        extendedSqsWithGenericReservedAttributeName = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfigurationWithGenericReservedAttributeName));
    }

    @Test
    public void testWhenSendMessageWithLargePayloadSupportDisabledThenS3IsNotUsedAndSqsBackendIsResponsibleToFailItWithDeprecatedMethod() {
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportDisabled();
        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = SendMessageRequest.builder()
            .queueUrl(SQS_QUEUE_URL)
            .messageBody(messageBody)
            .overrideConfiguration(
                AwsRequestOverrideConfiguration.builder()
                    .addApiName(ApiName.builder().name(USER_AGENT_NAME).version(USER_AGENT_VERSION).build())
                    .build())
            .build();
        sqsExtended.sendMessage(messageRequest);

        ArgumentCaptor<SendMessageRequest> argumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(mockS3, never()).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
        verify(mockSqsBackend).sendMessage(argumentCaptor.capture());
        assertEquals(messageRequest.queueUrl(), argumentCaptor.getValue().queueUrl());
        assertEquals(messageRequest.messageBody(), argumentCaptor.getValue().messageBody());
        assertEquals(messageRequest.overrideConfiguration().get().apiNames().get(0).name(), argumentCaptor.getValue().overrideConfiguration().get().apiNames().get(0).name());
        assertEquals(messageRequest.overrideConfiguration().get().apiNames().get(0).version(), argumentCaptor.getValue().overrideConfiguration().get().apiNames().get(0).version());
    }

    @Test
    public void testWhenSendMessageWithAlwaysThroughS3AndMessageIsSmallThenItIsStillStoredInS3WithDeprecatedMethod() {
        int messageLength = LESS_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withAlwaysThroughS3(true);
        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        sqsExtended.sendMessage(messageRequest).join();

        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
    }

    @Test
    public void testWhenSendMessageWithSetMessageSizeThresholdThenThresholdIsHonoredWithDeprecatedMethod() {
        int messageLength = ARBITRARY_SMALLER_THRESHOLD * 2;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withPayloadSizeThreshold(ARBITRARY_SMALLER_THRESHOLD);

        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        sqsExtended.sendMessage(messageRequest).join();
        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
    }

    @Test
    public void testReceiveMessageMultipleTimesDoesNotAdditionallyAlterReceiveMessageRequestWithDeprecatedMethod() {
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME);
        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build()));

        ReceiveMessageRequest messageRequest = ReceiveMessageRequest.builder().build();
        ReceiveMessageRequest expectedRequest = ReceiveMessageRequest.builder().build();

        sqsExtended.receiveMessage(messageRequest).join();
        assertEquals(expectedRequest, messageRequest);

        sqsExtended.receiveMessage(messageRequest).join();
        assertEquals(expectedRequest, messageRequest);
    }

    @Test
    public void testWhenSendLargeMessageThenPayloadIsStoredInS3() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest).join();

        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
    }

    @Test
    public void testWhenSendLargeMessage_WithoutKMS_ThenPayloadIsStoredInS3AndKMSKeyIdIsNotUsed() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest).join();

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<AsyncRequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(mockS3, times(1)).putObject(putObjectRequestArgumentCaptor.capture(), requestBodyArgumentCaptor.capture());

        assertNull(putObjectRequestArgumentCaptor.getValue().serverSideEncryption());
        assertEquals(putObjectRequestArgumentCaptor.getValue().bucket(), S3_BUCKET_NAME);
    }

    @Test
    public void testWhenSendLargeMessage_WithCustomKMS_ThenPayloadIsStoredInS3AndCorrectKMSKeyIdIsNotUsed() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithCustomKMS.sendMessage(messageRequest).join();

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<AsyncRequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(mockS3, times(1)).putObject(putObjectRequestArgumentCaptor.capture(), requestBodyArgumentCaptor.capture());

        assertEquals(putObjectRequestArgumentCaptor.getValue().ssekmsKeyId(), S3_SERVER_SIDE_ENCRYPTION_KMS_KEY_ID);
        assertEquals(putObjectRequestArgumentCaptor.getValue().bucket(), S3_BUCKET_NAME);
    }

    @Test
    public void testWhenSendLargeMessage_WithDefaultKMS_ThenPayloadIsStoredInS3AndCorrectKMSKeyIdIsNotUsed() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithDefaultKMS.sendMessage(messageRequest).join();

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<AsyncRequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(mockS3, times(1)).putObject(putObjectRequestArgumentCaptor.capture(), requestBodyArgumentCaptor.capture());

        assertTrue(putObjectRequestArgumentCaptor.getValue().serverSideEncryption() != null &&
                   putObjectRequestArgumentCaptor.getValue().ssekmsKeyId() == null);
        assertEquals(putObjectRequestArgumentCaptor.getValue().bucket(), S3_BUCKET_NAME);
    }

    @Test
    public void testSendLargeMessageWithDefaultConfigThenLegacyReservedAttributeNameIsUsed(){
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);
        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest).join();

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().messageAttributes();
        assertTrue(attributes.containsKey(AmazonSQSExtendedClientUtil.LEGACY_RESERVED_ATTRIBUTE_NAME));
        assertFalse(attributes.containsKey(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME));

    }

    @Test
    public void testSendLargeMessageWithGenericReservedAttributeNameConfigThenGenericReservedAttributeNameIsUsed(){
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);
        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithGenericReservedAttributeName.sendMessage(messageRequest).join();

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().messageAttributes();
        assertTrue(attributes.containsKey(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME));
        assertFalse(attributes.containsKey(AmazonSQSExtendedClientUtil.LEGACY_RESERVED_ATTRIBUTE_NAME));
    }

    @Test
    public void testWhenSendSmallMessageThenS3IsNotUsed() {
        String messageBody = generateStringWithLength(SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest).join();

        verify(mockS3, never()).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
    }

    @Test
    public void testWhenSendMessageWithLargePayloadSupportDisabledThenS3IsNotUsedAndSqsBackendIsResponsibleToFailIt() {
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportDisabled();
        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = SendMessageRequest.builder()
            .queueUrl(SQS_QUEUE_URL)
            .messageBody(messageBody)
            .overrideConfiguration(
                AwsRequestOverrideConfiguration.builder()
                    .addApiName(ApiName.builder().name(USER_AGENT_NAME).version(USER_AGENT_VERSION).build())
                    .build())
            .build();
        sqsExtended.sendMessage(messageRequest).join();

        ArgumentCaptor<SendMessageRequest> argumentCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(mockS3, never()).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
        verify(mockSqsBackend).sendMessage(argumentCaptor.capture());
        assertEquals(messageRequest.queueUrl(), argumentCaptor.getValue().queueUrl());
        assertEquals(messageRequest.messageBody(), argumentCaptor.getValue().messageBody());
        assertEquals(messageRequest.overrideConfiguration().get().apiNames().get(0).name(), argumentCaptor.getValue().overrideConfiguration().get().apiNames().get(0).name());
        assertEquals(messageRequest.overrideConfiguration().get().apiNames().get(0).version(), argumentCaptor.getValue().overrideConfiguration().get().apiNames().get(0).version());
    }

    @Test
    public void testWhenSendMessageWithAlwaysThroughS3AndMessageIsSmallThenItIsStillStoredInS3() {
        String messageBody = generateStringWithLength(LESS_THAN_SQS_SIZE_LIMIT);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withAlwaysThroughS3(true);
        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        sqsExtended.sendMessage(messageRequest).join();

        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
    }

    @Test
    public void testWhenSendMessageWithSetMessageSizeThresholdThenThresholdIsHonored() {
        int messageLength = ARBITRARY_SMALLER_THRESHOLD * 2;
        String messageBody = generateStringWithLength(messageLength);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withPayloadSizeThreshold(ARBITRARY_SMALLER_THRESHOLD);

        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        sqsExtended.sendMessage(messageRequest).join();
        verify(mockS3, times(1)).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
    }

    @Test
    public void testReceiveMessageMultipleTimesDoesNotAdditionallyAlterReceiveMessageRequest() {
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build()));

        ReceiveMessageRequest messageRequest = ReceiveMessageRequest.builder().build();

        ReceiveMessageRequest expectedRequest = ReceiveMessageRequest.builder().build();

        extendedSqsWithDefaultConfig.receiveMessage(messageRequest).join();
        assertEquals(expectedRequest, messageRequest);

        extendedSqsWithDefaultConfig.receiveMessage(messageRequest).join();
        assertEquals(expectedRequest, messageRequest);
    }

    @Test
    public void testReceiveMessage_when_MessageIsLarge_legacyReservedAttributeUsed() throws Exception {
        testReceiveMessage_when_MessageIsLarge(AmazonSQSExtendedClientUtil.LEGACY_RESERVED_ATTRIBUTE_NAME);
    }

    @Test
    public void testReceiveMessage_when_MessageIsLarge_ReservedAttributeUsed() throws Exception {
        testReceiveMessage_when_MessageIsLarge(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);
    }

    @Test
    public void testReceiveMessage_when_MessageIsSmall() throws Exception {
        String expectedMessageAttributeName = "AnyMessageAttribute";
        String expectedMessage = "SmallMessage";
        Message message = Message.builder()
            .messageAttributes(ImmutableMap.of(expectedMessageAttributeName, MessageAttributeValue.builder().build()))
            .body(expectedMessage)
            .build();
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build()));

        ReceiveMessageRequest messageRequest = ReceiveMessageRequest.builder().build();
        ReceiveMessageResponse actualReceiveMessageResponse = extendedSqsWithDefaultConfig.receiveMessage(messageRequest).join();
        Message actualMessage = actualReceiveMessageResponse.messages().get(0);

        assertEquals(expectedMessage, actualMessage.body());
        assertTrue(actualMessage.messageAttributes().containsKey(expectedMessageAttributeName));
        assertFalse(actualMessage.messageAttributes().keySet().containsAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES));
        verifyNoInteractions(mockS3);
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
            int messageLength = messageLengthForCounter[i];
            String messageBody = generateStringWithLength(messageLength);
            SendMessageBatchRequestEntry entry = SendMessageBatchRequestEntry.builder()
                .id("entry_" + i)
                .messageBody(messageBody)
                .build();
            batchEntries.add(entry);
        }

        SendMessageBatchRequest
            batchRequest = SendMessageBatchRequest.builder().queueUrl(SQS_QUEUE_URL).entries(batchEntries).build();
        extendedSqsWithDefaultConfig.sendMessageBatch(batchRequest).join();

        // There should be 8 puts for the 8 messages above the threshold
        verify(mockS3, times(8)).putObject(isA(PutObjectRequest.class), isA(AsyncRequestBody.class));
    }

    @Test
    public void testWhenMessageBatchIsLargeS3PointerIsCorrectlySentToSQSAndNotOriginalMessage() {
        String messageBody = generateStringWithLength(LESS_THAN_SQS_SIZE_LIMIT);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withAlwaysThroughS3(true);

        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<SendMessageBatchRequestEntry>();
        for (int i = 0; i < 10; i++) {
            SendMessageBatchRequestEntry entry = SendMessageBatchRequestEntry.builder()
                .id("entry_" + i)
                .messageBody(messageBody)
                .build();
            batchEntries.add(entry);
        }
        SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder().queueUrl(SQS_QUEUE_URL).entries(batchEntries).build();

        sqsExtended.sendMessageBatch(batchRequest).join();

        ArgumentCaptor<SendMessageBatchRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
        verify(mockSqsBackend).sendMessageBatch(sendMessageRequestCaptor.capture());

        for (SendMessageBatchRequestEntry entry : sendMessageRequestCaptor.getValue().entries()) {
            assertNotEquals(messageBody, entry.messageBody());
        }
    }

    @Test
    public void testWhenSmallMessageIsSentThenNoAttributeIsAdded() {
        String messageBody = generateStringWithLength(LESS_THAN_SQS_SIZE_LIMIT);

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest).join();

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().messageAttributes();
        assertTrue(attributes.isEmpty());
    }

    @Test
    public void testWhenLargeMessageIsSentThenAttributeWithPayloadSizeIsAdded() {
        int messageLength = MORE_THAN_SQS_SIZE_LIMIT;
        String messageBody = generateStringWithLength(messageLength);

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        extendedSqsWithDefaultConfig.sendMessage(messageRequest).join();

        ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqsBackend).sendMessage(sendMessageRequestCaptor.capture());

        Map<String, MessageAttributeValue> attributes = sendMessageRequestCaptor.getValue().messageAttributes();
        assertEquals("Number", attributes.get(AmazonSQSExtendedClientUtil.LEGACY_RESERVED_ATTRIBUTE_NAME).dataType());
        assertEquals(messageLength, (int) Integer.parseInt(attributes.get(AmazonSQSExtendedClientUtil.LEGACY_RESERVED_ATTRIBUTE_NAME).stringValue()));
    }

    @Test
    public void testDefaultExtendedClientDeletesSmallMessage() {
        // given
        String receiptHandle = UUID.randomUUID().toString();
        DeleteMessageRequest
            deleteRequest = DeleteMessageRequest.builder().queueUrl(SQS_QUEUE_URL).receiptHandle(receiptHandle).build();

        // when
        extendedSqsWithDefaultConfig.deleteMessage(deleteRequest).join();

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
        extendedSqsWithDefaultConfig.deleteMessage(deleteRequest).join();

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

        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME, false);

        SqsAsyncClient extendedSqs = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        // when
        extendedSqs.deleteMessage(deleteRequest).join();

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
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME, false);
        SqsAsyncClient extendedSqs = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        // when
        extendedSqs.deleteMessageBatch(deleteBatchRequest).join();

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
        extendedSqsWithDefaultConfig.deleteMessageBatch(deleteBatchRequest).join();

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

    @Test
    public void testWhenSendMessageWIthCannedAccessControlListDefined() {
        ObjectCannedACL expected = ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL;
        String messageBody = generateStringWithLength(MORE_THAN_SQS_SIZE_LIMIT);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration()
            .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME).withObjectCannedACL(expected);
        SqsAsyncClient sqsExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedClientConfiguration));

        SendMessageRequest messageRequest = SendMessageRequest.builder().queueUrl(SQS_QUEUE_URL).messageBody(messageBody).build();
        sqsExtended.sendMessage(messageRequest).join();

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(mockS3).putObject(captor.capture(), any(AsyncRequestBody.class));

        assertEquals(expected, captor.getValue().acl());
    }

    private void testReceiveMessage_when_MessageIsLarge(String reservedAttributeName) throws Exception {
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

        ResponseBytes<GetObjectResponse> s3Object = ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(),
            expectedMessage.getBytes(StandardCharsets.UTF_8));
        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build()));
        when(mockS3.getObject(isA(GetObjectRequest.class), isA(AsyncResponseTransformer.class))).thenReturn(
            CompletableFuture.completedFuture(s3Object));

        ReceiveMessageRequest messageRequest = ReceiveMessageRequest.builder().build();
        ReceiveMessageResponse actualReceiveMessageResponse = extendedSqsWithDefaultConfig.receiveMessage(messageRequest).join();
        Message actualMessage = actualReceiveMessageResponse.messages().get(0);

        assertEquals(expectedMessage, actualMessage.body());
        assertFalse(actualMessage.messageAttributes().keySet().containsAll(AmazonSQSExtendedClientUtil.RESERVED_ATTRIBUTE_NAMES));
        verify(mockS3, times(1)).getObject(isA(GetObjectRequest.class), isA(AsyncResponseTransformer.class));
    }

    @Test
    public void testReceiveMessage_when_ignorePayloadNotFound_then_messageWithPayloadNotFoundIsDeletedFromSQS() {
        ExtendedAsyncClientConfiguration extendedAsyncClientConfiguration = new ExtendedAsyncClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withIgnorePayloadNotFound(true);
        SqsAsyncClient sqsAsyncExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedAsyncClientConfiguration));

        String receiptHandle = "receipt-handle";
        Message message = Message.builder()
                .messageAttributes(ImmutableMap.of(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME, MessageAttributeValue.builder().build()))
                .body(new PayloadS3Pointer(S3_BUCKET_NAME, "S3Key").toJson())
                .receiptHandle(receiptHandle)
                .build();

        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(
               CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build()));
        doThrow(NoSuchKeyException.class).when(mockS3).getObject((GetObjectRequest) any(), any(AsyncResponseTransformer.class));

        ReceiveMessageRequest messageRequest = ReceiveMessageRequest.builder().queueUrl(SQS_QUEUE_URL).build();
        ReceiveMessageResponse receiveMessageResponse = sqsAsyncExtended.receiveMessage(messageRequest).join();

        assertTrue(receiveMessageResponse.messages().isEmpty());

        ArgumentCaptor<DeleteMessageRequest> deleteMessageRequestArgumentCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(mockSqsBackend).deleteMessage(deleteMessageRequestArgumentCaptor.capture());
        assertEquals(SQS_QUEUE_URL, deleteMessageRequestArgumentCaptor.getValue().queueUrl());
        assertEquals(receiptHandle, deleteMessageRequestArgumentCaptor.getValue().receiptHandle());
    }

    @Test
    public void testReceiveMessage_when_ignorePayloadNotFoundIsFalse_then_messageWithPayloadNotFoundThrowsException() {
        ExtendedAsyncClientConfiguration extendedAsyncClientConfiguration = new ExtendedAsyncClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withIgnorePayloadNotFound(false);
        SqsAsyncClient sqsAsyncExtended = spy(new AmazonSQSExtendedAsyncClient(mockSqsBackend, extendedAsyncClientConfiguration));

        String receiptHandle = "receipt-handle";
        Message message = Message.builder()
                .messageAttributes(ImmutableMap.of(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME, MessageAttributeValue.builder().build()))
                .body(new PayloadS3Pointer(S3_BUCKET_NAME, "S3Key").toJson())
                .receiptHandle(receiptHandle)
                .build();

        when(mockSqsBackend.receiveMessage(isA(ReceiveMessageRequest.class))).thenReturn(
                CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build()));
        doThrow(NoSuchKeyException.class).when(mockS3).getObject((GetObjectRequest) any(), any(AsyncResponseTransformer.class));

        ReceiveMessageRequest messageRequest = ReceiveMessageRequest.builder().build();
        try {
            sqsAsyncExtended.receiveMessage(messageRequest).join();
            fail("Expected exception after receiving NoSuchKeyException from S3 was not thrown.");
        } catch (CompletionException e) {
            assertEquals(NoSuchKeyException.class.getName(), e.getCause().getClass().getName());
            verify(mockSqsBackend, never()).deleteMessage(any(DeleteMessageRequest.class));
        }
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

    private String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }
}
