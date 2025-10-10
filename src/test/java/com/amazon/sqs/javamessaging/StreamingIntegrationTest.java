package com.amazon.sqs.javamessaging;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for streaming functionality using LocalStack.
 * This test requires LocalStack to be running.
 */
@EnabledIfEnvironmentVariable(named = "LOCALSTACK_ENDPOINT", matches = ".*")
public class StreamingIntegrationTest {

    private static final String LOCALSTACK_ENDPOINT = System.getenv("LOCALSTACK_ENDPOINT") != null ?
        System.getenv("LOCALSTACK_ENDPOINT") : "http://localhost:4566";
    private static final String BUCKET_NAME = "offload-bucket";
    private static final String QUEUE_NAME = "streaming-test-queue";

    private static SqsClient sqsClient;
    private static S3Client s3Client;
    private static AmazonSQSExtendedClient extendedClient;
    private static String queueUrl;

    @BeforeAll
    public static void setup() {
        // Create clients pointing to LocalStack
        sqsClient = SqsClient.builder()
            .endpointOverride(URI.create(LOCALSTACK_ENDPOINT))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("dummy", "dummy")))
            .region(Region.of("ap-southeast-1"))
            .build();

        s3Client = S3Client.builder()
            .endpointOverride(URI.create(LOCALSTACK_ENDPOINT))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("dummy", "dummy")))
            .region(Region.of("ap-southeast-1"))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .build();

        // Create S3AsyncClient with same configuration for TransferManager
        // software.amazon.awssdk.services.s3.S3AsyncClient s3AsyncClient = 
        //     software.amazon.awssdk.services.s3.S3AsyncClient.builder()
        //         .endpointOverride(URI.create(LOCALSTACK_ENDPOINT))
        //         .credentialsProvider(StaticCredentialsProvider.create(
        //             AwsBasicCredentials.create("dummy", "dummy")))
        //         .region(Region.of("ap-southeast-1"))
        //         .serviceConfiguration(S3Configuration.builder()
        //             .pathStyleAccessEnabled(true)
        //             .build())
        //         .multipartEnabled(true)
        //         .multipartConfiguration(conf -> conf
        //             .minimumPartSizeInBytes(10 * 1024 * 1024L) // 10MB
        //             .thresholdInBytes(16 * 1024 * 1024L)) // 16MB
        //         .build();

        // Create extended client with stream support
        ExtendedClientConfiguration config = new ExtendedClientConfiguration()
            .withPayloadSupportEnabled(s3Client, BUCKET_NAME)
            .withStreamUploadEnabled(true)
            .withStreamUploadThreshold(5 * 1024 * 1024) // 5MB
            .withStreamUploadPartSize(10 * 1024 * 1024) // 10MB
            .withPayloadSizeThreshold(256 * 1024) // 256KB
            .withS3Region("ap-southeast-1");

        System.out.println("S3 Region configured: " + config.getS3Region());

        extendedClient = new AmazonSQSExtendedClient(sqsClient, config);

        // Create queue
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
            .queueName(QUEUE_NAME)
            .build();
        sqsClient.createQueue(createQueueRequest);

        // Get queue URL
        GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
            .queueName(QUEUE_NAME)
            .build();
        queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();

        System.out.println("Integration test setup complete. Using LocalStack at: " + LOCALSTACK_ENDPOINT);
    }

    @AfterAll
    public static void cleanup() {
        if (queueUrl != null) {
            try {
                DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();
                sqsClient.deleteQueue(deleteQueueRequest);
            } catch (Exception e) {
                System.err.println("Failed to delete queue: " + e.getMessage());
            }
        }

        if (sqsClient != null) {
            sqsClient.close();
        }
        if (s3Client != null) {
            s3Client.close();
        }
        if (extendedClient != null) {
            extendedClient.close();
        }
    }

    // @Test
    // public void testSendAndReceiveLargeMessageWithStreaming() throws IOException {
    //     // Create a large message (6MB) that will trigger stream upload
    //     String largeMessage = generateLargeMessage(6 * 1024 * 1024); // 6MB

    //     // Send the large message
    //     SendMessageRequest sendRequest = SendMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .messageBody(largeMessage)
    //         .build();

    //     extendedClient.sendMessage(sendRequest);
    //     System.out.println("Sent large message (" + largeMessage.length() + " chars, ~" +
    //         (largeMessage.length() * 2 / 1024 / 1024) + "MB)");

    //     // Receive the message using streaming
    //     ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .maxNumberOfMessages(1)
    //         .build();

    //     ReceiveStreamMessageResponse streamResponse = extendedClient.receiveMessageAsStream(receiveRequest);

    //     assertEquals(1, streamResponse.streamMessages().size(),
    //         "Should receive exactly one message");

    //     StreamMessage streamMessage = streamResponse.streamMessages().get(0);

    //     assertTrue(streamMessage.hasStreamPayload(),
    //         "Large message should have streaming payload");

    //     ResponseInputStream<GetObjectResponse> payloadStream = streamMessage.getPayloadStream();
    //     assertNotNull(payloadStream, "Payload stream should not be null");

    //     String receivedContent = readStreamContent(payloadStream);

    //     assertEquals(largeMessage, receivedContent,
    //         "Received content should match sent message");

    //     System.out.println("Successfully received and streamed large message content");
    // }

    // @Test
    // public void testSendAndReceiveSmallMessageWithoutStreaming() {
    //     String smallMessage = "This is a small message that stays in SQS";

    //     SendMessageRequest sendRequest = SendMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .messageBody(smallMessage)
    //         .build();

    //     extendedClient.sendMessage(sendRequest);
    //     System.out.println("Sent small message (" + smallMessage.length() + " chars)");

    //     // Receive the message using streaming
    //     ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .maxNumberOfMessages(1)
    //         .build();

    //     ReceiveStreamMessageResponse streamResponse = extendedClient.receiveMessageAsStream(receiveRequest);

    //     assertEquals(1, streamResponse.streamMessages().size(),
    //         "Should receive exactly one message");

    //     StreamMessage streamMessage = streamResponse.streamMessages().get(0);

    //     assertFalse(streamMessage.hasStreamPayload(),
    //         "Small message should not have streaming payload");

    //     assertEquals(smallMessage, streamMessage.getMessage().body(),
    //         "Small message content should be in message body");

    //     System.out.println("Successfully received small message without streaming");
    // }

    // @Test
    // public void testMixedMessageTypes() throws IOException {
    //     String smallMessage = "Small message";
    //     String largeMessage = generateLargeMessage(4 * 1024 * 1024); // 4MB

    //     extendedClient.sendMessage(SendMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .messageBody(smallMessage)
    //         .build());

    //     extendedClient.sendMessage(SendMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .messageBody(largeMessage)
    //         .build());

    //     ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .maxNumberOfMessages(10)
    //         .build();

    //     ReceiveStreamMessageResponse streamResponse = extendedClient.receiveMessageAsStream(receiveRequest);

    //     assertEquals(2, streamResponse.streamMessages().size(),
    //         "Should receive exactly two messages");

    //     StreamMessage smallStreamMessage = null;
    //     StreamMessage largeStreamMessage = null;

    //     for (StreamMessage msg : streamResponse.streamMessages()) {
    //         if (msg.hasStreamPayload()) {
    //             largeStreamMessage = msg;
    //         } else {
    //             smallStreamMessage = msg;
    //         }
    //     }

    //     assertNotNull(smallStreamMessage, "Should have small message");
    //     assertNotNull(largeStreamMessage, "Should have large message");

    //     assertEquals(smallMessage, smallStreamMessage.getMessage().body());

    //     ResponseInputStream<GetObjectResponse> payloadStream = largeStreamMessage.getPayloadStream();
    //     String receivedLargeContent = readStreamContent(payloadStream);
    //     assertEquals(largeMessage, receivedLargeContent);

    //     System.out.println("Successfully handled mixed small and large messages");
    // }

    // @Test
    // public void testStreamingUploadWithMultipartConfiguration() throws IOException {
    //     // Create a 20MB message that will definitely trigger multipart upload
    //     // With 10MB part size and 16MB threshold configured
    //     String veryLargeMessage = generateLargeMessage(20 * 1024 * 1024); // 20MB
    //     byte[] messageBytes = veryLargeMessage.getBytes(StandardCharsets.UTF_8);
        
    //     System.out.println("Sending very large message (" + 
    //         (messageBytes.length / 1024 / 1024) + "MB) - should use multipart upload");

    //     // Send the message using sendStreamMessage - this should trigger streaming upload with multipart
    //     SendMessageRequest sendRequest = SendMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .build();

    //     long startTime = System.currentTimeMillis();
    //     java.io.InputStream messageStream = new java.io.ByteArrayInputStream(messageBytes);
    //     extendedClient.sendStreamMessage(sendRequest, messageStream, messageBytes.length);
    //     long uploadTime = System.currentTimeMillis() - startTime;
        
    //     System.out.println("Upload completed in " + uploadTime + "ms using TransferManager with multipart");

    //     // Receive using streaming to avoid loading entire message into memory
    //     ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .maxNumberOfMessages(1)
    //         .waitTimeSeconds(10)
    //         .build();

    //     startTime = System.currentTimeMillis();
    //     ReceiveStreamMessageResponse streamResponse = extendedClient.receiveMessageAsStream(receiveRequest);
    //     long receiveTime = System.currentTimeMillis() - startTime;

    //     System.out.println("Receive completed in " + receiveTime + "ms");

    //     assertEquals(1, streamResponse.streamMessages().size(),
    //         "Should receive exactly one message");

    //     StreamMessage streamMessage = streamResponse.streamMessages().get(0);

    //     assertTrue(streamMessage.hasStreamPayload(),
    //         "Very large message should have streaming payload");

    //     ResponseInputStream<GetObjectResponse> payloadStream = streamMessage.getPayloadStream();
    //     assertNotNull(payloadStream, "Payload stream should not be null");

    //     // Read the content in chunks to demonstrate true streaming
    //     startTime = System.currentTimeMillis();
    //     String receivedContent = readStreamContent(payloadStream);
    //     long readTime = System.currentTimeMillis() - startTime;

    //     System.out.println("Stream read completed in " + readTime + "ms");

    //     assertEquals(veryLargeMessage.length(), receivedContent.length(),
    //         "Received content length should match sent message length");
        
    //     // Verify start and end markers to ensure content integrity
    //     assertTrue(receivedContent.startsWith("START:"),
    //         "Content should start with START marker");
    //     assertTrue(receivedContent.endsWith(":END"),
    //         "Content should end with END marker");

    //     System.out.println("Successfully sent and received 20MB message using multipart streaming");
    // }

    // @Test
    // public void testStreamingWithCustomPartSizeAndThreshold() throws IOException {
    //     // This test verifies that the configured part size (10MB) and threshold (16MB)
    //     // are being used correctly for multipart uploads
        
    //     // Create a message just above the threshold (17MB)
    //     String largeMessage = generateLargeMessage(17 * 1024 * 1024); // 17MB
    //     byte[] messageBytes = largeMessage.getBytes(StandardCharsets.UTF_8);
        
    //     System.out.println("Testing with " + (messageBytes.length / 1024 / 1024) + 
    //         "MB message - above 16MB threshold, should trigger multipart");

    //     SendMessageRequest sendRequest = SendMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .build();

    //     java.io.InputStream messageStream = new java.io.ByteArrayInputStream(messageBytes);
    //     extendedClient.sendStreamMessage(sendRequest, messageStream, messageBytes.length);
    //     System.out.println("Successfully sent " + (messageBytes.length / 1024 / 1024) + 
    //         "MB message with multipart upload");

    //     // Receive and verify
    //     ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
    //         .queueUrl(queueUrl)
    //         .maxNumberOfMessages(1)
    //         .build();

    //     ReceiveStreamMessageResponse streamResponse = extendedClient.receiveMessageAsStream(receiveRequest);

    //     assertEquals(1, streamResponse.streamMessages().size());
    //     StreamMessage streamMessage = streamResponse.streamMessages().get(0);

    //     assertTrue(streamMessage.hasStreamPayload(),
    //         "Message above threshold should have streaming payload");

    //     String receivedContent = readStreamContent(streamMessage.getPayloadStream());
        
    //     assertEquals(largeMessage.length(), receivedContent.length(),
    //         "Received content should match sent message length");

    //     System.out.println("Successfully verified custom part size and threshold configuration");
    // }

    @Test
    public void testMemoryUsageComparisonSendTraditionalVsStreaming() throws IOException, InterruptedException {
        // Test 1: Compare memory usage for SENDING large messages
        // In real-world: sender and receiver are on different machines with separate memory
        
        final int messageSizeBytes = 50 * 1024 * 1024; // 50MB
        
        System.out.println("\n=== SEND Memory Usage Comparison ===");
        System.out.println("Message size: " + (messageSizeBytes / 1024 / 1024) + "MB");
        System.out.println("Comparing traditional sendMessage() vs sendStreamMessage()");
        
        Runtime runtime = Runtime.getRuntime();
        
        // TRADITIONAL SEND TEST FIRST
        System.out.println("\n--- Traditional sendMessage() ---");
        
        System.gc();
        Thread.sleep(200);
        long traditionalStartMemory = runtime.totalMemory() - runtime.freeMemory();
        long traditionalPeakMemory = traditionalStartMemory;
        System.out.println("Baseline memory: " + (traditionalStartMemory / 1024 / 1024) + "MB");
        
        String largeMessage = generateLargeMessage(messageSizeBytes);
        long afterLoadMemory = runtime.totalMemory() - runtime.freeMemory();
        traditionalPeakMemory = Math.max(traditionalPeakMemory, afterLoadMemory);
        System.out.println("After loading file into String: " + (afterLoadMemory / 1024 / 1024) + "MB (+" + 
            ((afterLoadMemory - traditionalStartMemory) / 1024 / 1024) + "MB)");
        
        SendMessageRequest sendRequest1 = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(largeMessage)
            .build();
        
        long beforeSend = System.currentTimeMillis();
        extendedClient.sendMessage(sendRequest1);
        long sendTime = System.currentTimeMillis() - beforeSend;
        
        long afterSendMemory = runtime.totalMemory() - runtime.freeMemory();
        traditionalPeakMemory = Math.max(traditionalPeakMemory, afterSendMemory);
        
        System.out.println("Send time: " + sendTime + "ms");
        System.out.println("Memory after send: " + (afterSendMemory / 1024 / 1024) + "MB (+" + 
            ((afterSendMemory - traditionalStartMemory) / 1024 / 1024) + "MB)");
        System.out.println("Peak memory: " + (traditionalPeakMemory / 1024 / 1024) + "MB (+" + 
            ((traditionalPeakMemory - traditionalStartMemory) / 1024 / 1024) + "MB)");
        
        assertTrue(largeMessage.startsWith("START:"), "Traditional send message should start with START:");
        assertTrue(largeMessage.endsWith(":END"), "Traditional send message should end with :END");
        
        long traditionalSendMemoryUsed = traditionalPeakMemory - traditionalStartMemory;
        
        largeMessage = null;
        System.gc();
        Thread.sleep(200);
        
        // STREAMING SEND TEST SECOND (fresh memory state)
        System.out.println("\n--- Streaming sendStreamMessage() ---");
        
        System.gc();
        Thread.sleep(200);
        long streamingStartMemory = runtime.totalMemory() - runtime.freeMemory();
        long streamingPeakMemory = streamingStartMemory;
        System.out.println("Baseline memory: " + (streamingStartMemory / 1024 / 1024) + "MB");
        
        byte[] messageBytes = generateLargeMessage(messageSizeBytes).getBytes(StandardCharsets.UTF_8);
        long afterGenerateMemory = runtime.totalMemory() - runtime.freeMemory();
        streamingPeakMemory = Math.max(streamingPeakMemory, afterGenerateMemory);
        System.out.println("After generating bytes for stream: " + (afterGenerateMemory / 1024 / 1024) + "MB (+" + 
            ((afterGenerateMemory - streamingStartMemory) / 1024 / 1024) + "MB)");
        
        SendMessageRequest sendRequest2 = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .build();
        
        long beforeStreamSend = System.currentTimeMillis();
        java.io.InputStream messageStream = new java.io.ByteArrayInputStream(messageBytes);
        extendedClient.sendStreamMessage(sendRequest2, messageStream, messageBytes.length);
        long streamSendTime = System.currentTimeMillis() - beforeStreamSend;
        
        long afterStreamSendMemory = runtime.totalMemory() - runtime.freeMemory();
        streamingPeakMemory = Math.max(streamingPeakMemory, afterStreamSendMemory);
        
        System.out.println("Send time: " + streamSendTime + "ms");
        System.out.println("Memory after send: " + (afterStreamSendMemory / 1024 / 1024) + "MB (+" + 
            ((afterStreamSendMemory - streamingStartMemory) / 1024 / 1024) + "MB)");
        System.out.println("Peak memory: " + (streamingPeakMemory / 1024 / 1024) + "MB (+" + 
            ((streamingPeakMemory - streamingStartMemory) / 1024 / 1024) + "MB)");
        
        String messageBytesStr = new String(messageBytes, StandardCharsets.UTF_8);
        assertTrue(messageBytesStr.startsWith("START:"), "Streaming send message should start with START:");
        assertTrue(messageBytesStr.endsWith(":END"), "Streaming send message should end with :END");
        messageBytesStr = null;
        
        long streamingSendMemoryUsed = streamingPeakMemory - streamingStartMemory;
        
        messageBytes = null;
        System.gc();
        Thread.sleep(200);
        
        System.out.println("\n=== SEND Comparison ===");
        System.out.println("Traditional send peak: " + (traditionalSendMemoryUsed / 1024 / 1024) + "MB");
        System.out.println("Streaming send peak: " + (streamingSendMemoryUsed / 1024 / 1024) + "MB");
        long sendMemorySaved = traditionalSendMemoryUsed - streamingSendMemoryUsed;
        double sendPercentSaved = traditionalSendMemoryUsed > 0 ? 
            (sendMemorySaved * 100.0) / traditionalSendMemoryUsed : 0;
        System.out.println("Memory saved: " + (sendMemorySaved / 1024 / 1024) + "MB (" + 
            String.format("%.1f", sendPercentSaved) + "% reduction)");
    }
    
    @Test
    public void testRealWorldStreamingVsTraditionalReceive() throws IOException, InterruptedException {
        // REAL-WORLD SCENARIO: Compare memory usage when actually PROCESSING the content
        // This simulates what happens in real applications where you need to consume the data

        final int messageSizeBytes = 50 * 1024 * 1024; // 50MB

        System.out.println("\n=== REAL-WORLD RECEIVE Comparison ===");
        System.out.println("Message size: " + (messageSizeBytes / 1024 / 1024) + "MB");
        System.out.println("Simulating real app: processing content (counting chars, validating data)");

        Runtime runtime = Runtime.getRuntime();

        // TRADITIONAL APPROACH: Load entire content, then process it
        String largeMessage = generateLargeMessage(messageSizeBytes);
        SendMessageRequest sendRequest1 = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(largeMessage)
            .build();
        extendedClient.sendMessage(sendRequest1);
        largeMessage = null; // Release reference
        System.gc();
        Thread.sleep(200);

        System.out.println("\n--- Traditional: Load entire content, then process ---");

        System.gc();
        Thread.sleep(200);
        long traditionalStartMemory = runtime.totalMemory() - runtime.freeMemory();
        long traditionalPeakMemory = traditionalStartMemory;
        System.out.println("Baseline memory: " + (traditionalStartMemory / 1024 / 1024) + "MB");

        ReceiveMessageRequest receiveRequest1 = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .build();

        long beforeReceive = System.currentTimeMillis();
        software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse response =
            extendedClient.receiveMessage(receiveRequest1);
        long receiveTime = System.currentTimeMillis() - beforeReceive;

        long afterReceiveMemory = runtime.totalMemory() - runtime.freeMemory();
        traditionalPeakMemory = Math.max(traditionalPeakMemory, afterReceiveMemory);

        // REAL PROCESSING: Access the body (loads entire content)
        String receivedBody = response.messages().get(0).body();
        long afterBodyAccessMemory = runtime.totalMemory() - runtime.freeMemory();
        traditionalPeakMemory = Math.max(traditionalPeakMemory, afterBodyAccessMemory);

        // Simulate real processing: count characters, validate content
        long beforeProcessing = System.currentTimeMillis();
        int charCount = receivedBody.length();
        boolean hasValidContent = receivedBody.contains("START:") && receivedBody.contains(":END");
        int dataLines = 0;
        for (char c : receivedBody.toCharArray()) {
            if (c == '\n') dataLines++;
        }
        long processingTime = System.currentTimeMillis() - beforeProcessing;

        long afterProcessingMemory = runtime.totalMemory() - runtime.freeMemory();
        traditionalPeakMemory = Math.max(traditionalPeakMemory, afterProcessingMemory);

        System.out.println("Receive time: " + receiveTime + "ms");
        System.out.println("Processing time: " + processingTime + "ms");
        System.out.println("Content length: " + charCount + " chars");
        System.out.println("Data lines: " + dataLines);
        System.out.println("Valid content: " + hasValidContent);
        System.out.println("Memory after receive: " + (afterReceiveMemory / 1024 / 1024) + "MB (+" +
            ((afterReceiveMemory - traditionalStartMemory) / 1024 / 1024) + "MB)");
        System.out.println("Memory after body access: " + (afterBodyAccessMemory / 1024 / 1024) + "MB (+" +
            ((afterBodyAccessMemory - traditionalStartMemory) / 1024 / 1024) + "MB)");
        System.out.println("Memory after processing: " + (afterProcessingMemory / 1024 / 1024) + "MB (+" +
            ((afterProcessingMemory - traditionalStartMemory) / 1024 / 1024) + "MB)");
        System.out.println("Peak memory: " + (traditionalPeakMemory / 1024 / 1024) + "MB (+" +
            ((traditionalPeakMemory - traditionalStartMemory) / 1024 / 1024) + "MB)");

        long traditionalMemoryUsed = traditionalPeakMemory - traditionalStartMemory;

        receivedBody = null;
        response = null;
        System.gc();
        Thread.sleep(200);

        // STREAMING APPROACH: Process content as it streams (realistic scenario)
        byte[] messageBytes = generateLargeMessage(messageSizeBytes).getBytes(StandardCharsets.UTF_8);
        SendMessageRequest sendRequest2 = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .build();
        java.io.InputStream sendStream = new java.io.ByteArrayInputStream(messageBytes);
        extendedClient.sendStreamMessage(sendRequest2, sendStream, messageBytes.length);
        messageBytes = null;
        System.gc();
        Thread.sleep(200);

        System.out.println("\n--- Streaming: Process content as it streams ---");

        System.gc();
        Thread.sleep(200);
        long streamingStartMemory = runtime.totalMemory() - runtime.freeMemory();
        long streamingPeakMemory = streamingStartMemory;
        System.out.println("Baseline memory: " + (streamingStartMemory / 1024 / 1024) + "MB");

        ReceiveMessageRequest receiveRequest2 = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .build();

        long beforeStreamReceive = System.currentTimeMillis();
        ReceiveStreamMessageResponse streamResponse =
            extendedClient.receiveMessageAsStream(receiveRequest2);
        long streamReceiveTime = System.currentTimeMillis() - beforeStreamReceive;

        long afterStreamReceiveMemory = runtime.totalMemory() - runtime.freeMemory();
        streamingPeakMemory = Math.max(streamingPeakMemory, afterStreamReceiveMemory);

        System.out.println("Receive time: " + streamReceiveTime + "ms");
        System.out.println("Memory after receive: " + (afterStreamReceiveMemory / 1024 / 1024) + "MB (+" +
            ((afterStreamReceiveMemory - streamingStartMemory) / 1024 / 1024) + "MB)");

        // REAL STREAMING PROCESSING: Process content as it streams
        StreamMessage streamMessage = streamResponse.streamMessages().get(0);
        ResponseInputStream<GetObjectResponse> payloadStream = streamMessage.getPayloadStream();

        long beforeStreamProcessing = System.currentTimeMillis();
        long totalBytesRead = 0;
        int streamCharCount = 0;
        boolean streamHasValidContent = false;
        int streamDataLines = 0;
        boolean foundStart = false;
        boolean foundEnd = false;

        try (ResponseInputStream<GetObjectResponse> s = payloadStream) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            
            // Patterns to search for anywhere in the stream
            String startPattern = "START:";
            String endPattern = ":END";
            
            // Sliding window to handle patterns that span chunk boundaries
            byte[] previousChunkTail = new byte[0];
            int maxPatternLength = Math.max(startPattern.length(), endPattern.length());

            while ((bytesRead = s.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                streamCharCount += bytesRead;

                // Count newlines by scanning bytes directly (memory efficient)
                for (int i = 0; i < bytesRead; i++) {
                    if (buffer[i] == '\n') streamDataLines++;
                }

                // Pattern matching: search in current chunk + overlap from previous chunk
                if (!foundStart || !foundEnd) {
                    // Combine previous chunk tail with current chunk for pattern search
                    // This handles patterns that span chunk boundaries
                    byte[] searchBuffer;
                    
                    if (previousChunkTail.length > 0) {
                        searchBuffer = new byte[previousChunkTail.length + bytesRead];
                        System.arraycopy(previousChunkTail, 0, searchBuffer, 0, previousChunkTail.length);
                        System.arraycopy(buffer, 0, searchBuffer, previousChunkTail.length, bytesRead);
                    } else {
                        searchBuffer = buffer;
                    }
                    
                    int searchLength = (searchBuffer == buffer) ? bytesRead : searchBuffer.length;
                    String chunk = new String(searchBuffer, 0, searchLength, StandardCharsets.UTF_8);
                    
                    if (!foundStart && chunk.contains(startPattern)) {
                        foundStart = true;
                    }
                    if (!foundEnd && chunk.contains(endPattern)) {
                        foundEnd = true;
                    }
                    
                    chunk = null; // Release immediately
                    
                    // Save tail of current chunk for next iteration (to handle boundary patterns)
                    int tailLength = Math.min(bytesRead, maxPatternLength);
                    previousChunkTail = new byte[tailLength];
                    System.arraycopy(buffer, bytesRead - tailLength, previousChunkTail, 0, tailLength);
                }

                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                streamingPeakMemory = Math.max(streamingPeakMemory, currentMemory);
            }
        }

        streamHasValidContent = foundStart && foundEnd;
        long streamProcessingTime = System.currentTimeMillis() - beforeStreamProcessing;

        long afterStreamProcessingMemory = runtime.totalMemory() - runtime.freeMemory();
        streamingPeakMemory = Math.max(streamingPeakMemory, afterStreamProcessingMemory);

        System.out.println("Processing time: " + streamProcessingTime + "ms");
        System.out.println("Total bytes: " + (totalBytesRead / 1024 / 1024) + "MB");
        System.out.println("Content length: " + streamCharCount + " chars");
        System.out.println("Data lines: " + streamDataLines);
        System.out.println("Valid content: " + streamHasValidContent);
        System.out.println("Memory after processing: " + (afterStreamProcessingMemory / 1024 / 1024) + "MB (+" +
            ((afterStreamProcessingMemory - streamingStartMemory) / 1024 / 1024) + "MB)");
        System.out.println("Peak memory: " + (streamingPeakMemory / 1024 / 1024) + "MB (+" +
            ((streamingPeakMemory - streamingStartMemory) / 1024 / 1024) + "MB)");

        long streamingMemoryUsed = streamingPeakMemory - streamingStartMemory;

        System.out.println("\n=== REAL-WORLD Comparison ===");
        System.out.println("Traditional peak: " + (traditionalMemoryUsed / 1024 / 1024) + "MB");
        System.out.println("Streaming peak: " + (streamingMemoryUsed / 1024 / 1024) + "MB");
        long memorySaved = traditionalMemoryUsed - streamingMemoryUsed;
        double percentSaved = traditionalMemoryUsed > 0 ?
            (memorySaved * 100.0) / traditionalMemoryUsed : 0;
        System.out.println("Memory saved: " + (memorySaved / 1024 / 1024) + "MB (" +
            String.format("%.1f", percentSaved) + "% reduction)");

        assertTrue(totalBytesRead >= messageSizeBytes - 100 && totalBytesRead <= messageSizeBytes + 100);
        assertEquals(charCount, streamCharCount, "Both approaches should count same characters");
        assertEquals(dataLines, streamDataLines, "Both approaches should count same lines");
        assertEquals(hasValidContent, streamHasValidContent, "Both approaches should validate content same way");
    }


    private String generateLargeMessage(int sizeInBytes) {
        int numChars = sizeInBytes;
        StringBuilder sb = new StringBuilder(numChars);
        sb.append("START:");
        for (int i = 0; i < numChars - 12; i++) {
            sb.append((char) ('A' + (i % 26)));
        }
        sb.append(":END");
        return sb.toString();
    }

    private String readStreamContent(ResponseInputStream<GetObjectResponse> stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (ResponseInputStream<GetObjectResponse> s = stream) {
            byte[] bytes = s.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}