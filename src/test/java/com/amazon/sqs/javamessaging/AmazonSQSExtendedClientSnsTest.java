package com.amazon.sqs.javamessaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.payloadoffloading.PayloadS3Pointer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmazonSQSExtendedClientSnsTest {

    private static final String S3_BUCKET_NAME = "test-bucket";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SqsClient mockSqs;
    private S3Client mockS3;
    private SqsAsyncClient mockSqsAsync;
    private S3AsyncClient mockS3Async;

    @BeforeEach
    public void setup() {
        mockSqs = mock(SqsClient.class);
        mockS3 = mock(S3Client.class);
        mockSqsAsync = mock(SqsAsyncClient.class);
        mockS3Async = mock(S3AsyncClient.class);
    }

    @Test
    public void testSyncReceiveMessageWithSnsAndS3Pointer() throws Exception {
        ExtendedClientConfiguration config = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withPayloadSupportFromSnsEnabled(true);
        AmazonSQSExtendedClient extendedClient = new AmazonSQSExtendedClient(mockSqs, config);

        String s3Pointer = new PayloadS3Pointer(S3_BUCKET_NAME, "s3-key").toJson();
        String escapedS3Pointer = s3Pointer.replace("\"", "\\\"");
        String snsJson = "{\"Type\":\"Notification\",\"Subject\":\"LargePayload\",\"Message\":\"" + escapedS3Pointer + "\"}";

        Message message = Message.builder()
                .body(snsJson)
                .messageAttributes(Collections.singletonMap(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME, 
                        MessageAttributeValue.builder().dataType("Number").stringValue("100").build()))
                .build();
        
        when(mockSqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        String largePayload = "ResolvedLargePayload";
        GetObjectResponse getObjectResponse = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(getObjectResponse, 
                AbortableInputStream.create(new StringInputStream(largePayload)));
        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        ReceiveMessageResponse result = extendedClient.receiveMessage(ReceiveMessageRequest.builder().build());
        String resultBody = result.messages().get(0).body();

        JsonNode root = MAPPER.readTree(resultBody);
        assertEquals("Notification", root.get("Type").asText());
        assertEquals("LargePayload", root.get("Subject").asText());
        assertEquals(largePayload, root.get("Message").asText());
    }

    @Test
    public void testAsyncReceiveMessageWithSnsAndS3Pointer() throws Exception {
        ExtendedAsyncClientConfiguration config = new ExtendedAsyncClientConfiguration()
                .withPayloadSupportEnabled(mockS3Async, S3_BUCKET_NAME)
                .withPayloadSupportFromSnsEnabled(true);
        AmazonSQSExtendedAsyncClient extendedClient = new AmazonSQSExtendedAsyncClient(mockSqsAsync, config);

        String s3Pointer = new PayloadS3Pointer(S3_BUCKET_NAME, "s3-key").toJson();
        String escapedS3Pointer = s3Pointer.replace("\"", "\\\"");
        String snsJson = "{\"Type\":\"Notification\",\"Message\":\"" + escapedS3Pointer + "\"}";

        Message message = Message.builder()
                .body(snsJson)
                .messageAttributes(Collections.singletonMap(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME, 
                        MessageAttributeValue.builder().dataType("Number").stringValue("100").build()))
                .build();

        when(mockSqsAsync.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build()));

        String largePayload = "ResolvedLargePayloadAsync";
        ResponseBytes<GetObjectResponse> s3Object = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                largePayload.getBytes(StandardCharsets.UTF_8));
        when(mockS3Async.getObject(isA(GetObjectRequest.class), isA(AsyncResponseTransformer.class)))
                .thenReturn(CompletableFuture.completedFuture(s3Object));

        ReceiveMessageResponse result = extendedClient.receiveMessage(ReceiveMessageRequest.builder().build()).get();
        String resultBody = result.messages().get(0).body();

        JsonNode root = MAPPER.readTree(resultBody);
        assertEquals(largePayload, root.get("Message").asText());
    }

    @Test
    public void testReceiveMessageStandardSqsWhenSnsEnabled() {
        ExtendedClientConfiguration config = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(mockS3, S3_BUCKET_NAME)
                .withPayloadSupportFromSnsEnabled(true);
        AmazonSQSExtendedClient extendedClient = new AmazonSQSExtendedClient(mockSqs, config);

        String standardBody = "Standard SQS Body";
        Message message = Message.builder().body(standardBody).build();
        
        when(mockSqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        ReceiveMessageResponse result = extendedClient.receiveMessage(ReceiveMessageRequest.builder().build());
        assertEquals(standardBody, result.messages().get(0).body());
    }
}
