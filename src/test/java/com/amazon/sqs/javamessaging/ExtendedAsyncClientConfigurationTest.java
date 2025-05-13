package com.amazon.sqs.javamessaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.payloadoffloading.ServerSideEncryptionFactory;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

/**
 * Tests the ExtendedAsyncClientConfiguration class.
 */
public class ExtendedAsyncClientConfigurationTest {

    private static final String s3BucketName = "test-bucket-name";
    private static final String s3ServerSideEncryptionKMSKeyId = "test-customer-managed-kms-key-id";
    private static final ServerSideEncryptionStrategy serverSideEncryptionStrategy =
        ServerSideEncryptionFactory.customerKey(s3ServerSideEncryptionKMSKeyId);

    @Test
    public void testCopyConstructor() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);

        boolean alwaysThroughS3 = true;
        int messageSizeThreshold = 500;
        boolean doesCleanupS3Payload = false;

        ExtendedAsyncClientConfiguration extendedClientConfig = new ExtendedAsyncClientConfiguration();

        extendedClientConfig.withPayloadSupportEnabled(s3, s3BucketName, doesCleanupS3Payload)
            .withAlwaysThroughS3(alwaysThroughS3).withPayloadSizeThreshold(messageSizeThreshold)
            .withServerSideEncryption(serverSideEncryptionStrategy);

        ExtendedAsyncClientConfiguration newExtendedClientConfig = new ExtendedAsyncClientConfiguration(extendedClientConfig);

        assertEquals(s3, newExtendedClientConfig.getS3AsyncClient());
        assertEquals(s3BucketName, newExtendedClientConfig.getS3BucketName());
        assertEquals(serverSideEncryptionStrategy, newExtendedClientConfig.getServerSideEncryptionStrategy());
        assertTrue(newExtendedClientConfig.isPayloadSupportEnabled());
        assertEquals(doesCleanupS3Payload, newExtendedClientConfig.doesCleanupS3Payload());
        assertEquals(alwaysThroughS3, newExtendedClientConfig.isAlwaysThroughS3());
        assertEquals(messageSizeThreshold, newExtendedClientConfig.getPayloadSizeThreshold());

        assertNotSame(newExtendedClientConfig, extendedClientConfig);
    }

    @Test
    public void testLargePayloadSupportEnabledWithDefaultDeleteFromS3Config() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName);

        assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        assertNotNull(extendedClientConfiguration.getS3AsyncClient());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Enabled() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, true);

        assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        assertNotNull(extendedClientConfiguration.getS3AsyncClient());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Disabled() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, false);

        assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        assertFalse(extendedClientConfiguration.doesCleanupS3Payload());
        assertNotNull(extendedClientConfiguration.getS3AsyncClient());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }
}
