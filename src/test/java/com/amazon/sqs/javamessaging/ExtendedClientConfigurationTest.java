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

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.payloadoffloading.ServerSideEncryptionFactory;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

import static org.mockito.Mockito.*;

/**
 * Tests the ExtendedClientConfiguration class.
 */
public class ExtendedClientConfigurationTest {

    private static String s3BucketName = "test-bucket-name";
    private static String s3ServerSideEncryptionKMSKeyId = "test-customer-managed-kms-key-id";
    private static ServerSideEncryptionStrategy serverSideEncryptionStrategy = ServerSideEncryptionFactory.customerKey(s3ServerSideEncryptionKMSKeyId);

    @Test
    public void testCopyConstructor() {
        S3Client s3 = mock(S3Client.class);

        boolean alwaysThroughS3 = true;
        int messageSizeThreshold = 500;
        boolean doesCleanupS3Payload = false;

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration();

        extendedClientConfig.withPayloadSupportEnabled(s3, s3BucketName, doesCleanupS3Payload)
                .withAlwaysThroughS3(alwaysThroughS3).withPayloadSizeThreshold(messageSizeThreshold)
                .withServerSideEncryption(serverSideEncryptionStrategy);

        ExtendedClientConfiguration newExtendedClientConfig = new ExtendedClientConfiguration(extendedClientConfig);

        Assert.assertEquals(s3, newExtendedClientConfig.getS3Client());
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
        S3Client s3 = mock(S3Client.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Enabled() {

        S3Client s3 = mock(S3Client.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, true);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Disabled() {
        S3Client s3 = mock(S3Client.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, false);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertFalse(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testCopyConstructorDeprecated() {

        S3Client s3 = mock(S3Client.class);
//        when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        boolean alwaysThroughS3 = true;
        int messageSizeThreshold = 500;

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration();

        extendedClientConfig.withLargePayloadSupportEnabled(s3, s3BucketName)
                .withAlwaysThroughS3(alwaysThroughS3).withMessageSizeThreshold(messageSizeThreshold);

        ExtendedClientConfiguration newExtendedClientConfig = new ExtendedClientConfiguration(extendedClientConfig);

        Assert.assertEquals(s3, newExtendedClientConfig.getS3Client());
        Assert.assertEquals(s3BucketName, newExtendedClientConfig.getS3BucketName());
        Assert.assertTrue(newExtendedClientConfig.isLargePayloadSupportEnabled());
        Assert.assertEquals(alwaysThroughS3, newExtendedClientConfig.isAlwaysThroughS3());
        Assert.assertEquals(messageSizeThreshold, newExtendedClientConfig.getMessageSizeThreshold());

        Assert.assertNotSame(newExtendedClientConfig, extendedClientConfig);
    }

    @Test
    public void testLargePayloadSupportEnabled() {

        S3Client s3 = mock(S3Client.class);
//        when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setLargePayloadSupportEnabled(s3, s3BucketName);

        Assert.assertTrue(extendedClientConfiguration.isLargePayloadSupportEnabled());
        Assert.assertNotNull(extendedClientConfiguration.getS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testDisableLargePayloadSupport() {

        S3Client s3 = mock(S3Client.class);
//        when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setLargePayloadSupportDisabled();

        Assert.assertNull(extendedClientConfiguration.getS3Client());
        Assert.assertNull(extendedClientConfiguration.getS3BucketName());

//        verify(s3, never()).putObject(isA(PutObjectRequest.class));
    }

    @Test
    public void testMessageSizeThreshold() {

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        Assert.assertEquals(SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD,
                extendedClientConfiguration.getMessageSizeThreshold());

        int messageLength = 1000;
        extendedClientConfiguration.setMessageSizeThreshold(messageLength);
        Assert.assertEquals(messageLength, extendedClientConfiguration.getMessageSizeThreshold());

    }
}
