package com.amazon.sqs.javamessaging;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.payloadoffloading.ServerSideEncryptionFactory;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

/**
 * Tests the ExtendedAsyncClientConfiguration class.
 */
public class ExtendedAsyncClientConfigurationTest {

    private static String s3BucketName = "test-bucket-name";
    private static String s3ServerSideEncryptionKMSKeyId = "test-customer-managed-kms-key-id";
    private static ServerSideEncryptionStrategy serverSideEncryptionStrategy = ServerSideEncryptionFactory.customerKey(s3ServerSideEncryptionKMSKeyId);

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

        Assert.assertEquals(s3, newExtendedClientConfig.getS3AsyncClient());
        Assert.assertEquals(s3BucketName, newExtendedClientConfig.getS3BucketName());
        Assert.assertEquals(serverSideEncryptionStrategy, newExtendedClientConfig.getServerSideEncryptionStrategy());
        Assert.assertTrue(newExtendedClientConfig.isPayloadSupportEnabled());
        Assert.assertEquals(doesCleanupS3Payload, newExtendedClientConfig.doesCleanupS3Payload());
        Assert.assertEquals(alwaysThroughS3, newExtendedClientConfig.isAlwaysThroughS3());
        Assert.assertEquals(messageSizeThreshold, newExtendedClientConfig.getPayloadSizeThreshold());

        Assert.assertNotSame(newExtendedClientConfig, extendedClientConfig);
    }

    @Test
    public void testLargePayloadSupportEnabledWithDefaultDeleteFromS3Config() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getS3AsyncClient());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Enabled() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, true);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getS3AsyncClient());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Disabled() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        ExtendedAsyncClientConfiguration extendedClientConfiguration = new ExtendedAsyncClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, false);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertFalse(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getS3AsyncClient());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }
}
