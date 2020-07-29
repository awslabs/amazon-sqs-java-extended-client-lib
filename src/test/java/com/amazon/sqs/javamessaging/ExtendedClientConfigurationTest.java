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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * Tests the ExtendedClientConfiguration class.
 */
public class ExtendedClientConfigurationTest {

    private static String s3BucketName = "test-bucket-name";
    private static String s3ServerSideEncryptionKMSKeyId = "test-customer-managed-kms-key-id";

    @Before
    public void setup() {
    }

    @Test
    public void testCopyConstructor() {
        AmazonS3 s3 = mock(AmazonS3.class);

        boolean alwaysThroughS3 = true;
        int messageSizeThreshold = 500;
        boolean doesCleanupS3Payload = false;

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration();

        extendedClientConfig.withPayloadSupportEnabled(s3, s3BucketName, doesCleanupS3Payload)
                .withAlwaysThroughS3(alwaysThroughS3).withPayloadSizeThreshold(messageSizeThreshold)
                .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(s3ServerSideEncryptionKMSKeyId));

        ExtendedClientConfiguration newExtendedClientConfig = new ExtendedClientConfiguration(extendedClientConfig);

        Assert.assertEquals(s3, newExtendedClientConfig.getAmazonS3Client());
        Assert.assertEquals(s3BucketName, newExtendedClientConfig.getS3BucketName());
        Assert.assertEquals(s3ServerSideEncryptionKMSKeyId, newExtendedClientConfig.getSSEAwsKeyManagementParams().getAwsKmsKeyId());
        Assert.assertTrue(newExtendedClientConfig.isPayloadSupportEnabled());
        Assert.assertEquals(doesCleanupS3Payload, newExtendedClientConfig.doesCleanupS3Payload());
        Assert.assertEquals(alwaysThroughS3, newExtendedClientConfig.isAlwaysThroughS3());
        Assert.assertEquals(messageSizeThreshold, newExtendedClientConfig.getPayloadSizeThreshold());

        Assert.assertNotSame(newExtendedClientConfig, extendedClientConfig);
    }

    @Test
    public void testLargePayloadSupportEnabledWithDefaultDeleteFromS3Config() {
        AmazonS3 s3 = mock(AmazonS3.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getAmazonS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Enabled() {

        AmazonS3 s3 = mock(AmazonS3.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, true);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getAmazonS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Disabled() {
        AmazonS3 s3 = mock(AmazonS3.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, false);

        Assert.assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        Assert.assertFalse(extendedClientConfiguration.doesCleanupS3Payload());
        Assert.assertNotNull(extendedClientConfiguration.getAmazonS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testCopyConstructorDeprecated() {

        AmazonS3 s3 = mock(AmazonS3.class);
        when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        boolean alwaysThroughS3 = true;
        int messageSizeThreshold = 500;

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration();

        extendedClientConfig.withLargePayloadSupportEnabled(s3, s3BucketName)
                .withAlwaysThroughS3(alwaysThroughS3).withMessageSizeThreshold(messageSizeThreshold);

        ExtendedClientConfiguration newExtendedClientConfig = new ExtendedClientConfiguration(extendedClientConfig);

        Assert.assertEquals(s3, newExtendedClientConfig.getAmazonS3Client());
        Assert.assertEquals(s3BucketName, newExtendedClientConfig.getS3BucketName());
        Assert.assertTrue(newExtendedClientConfig.isLargePayloadSupportEnabled());
        Assert.assertEquals(alwaysThroughS3, newExtendedClientConfig.isAlwaysThroughS3());
        Assert.assertEquals(messageSizeThreshold, newExtendedClientConfig.getMessageSizeThreshold());

        Assert.assertNotSame(newExtendedClientConfig, extendedClientConfig);
    }

    @Test
    public void testLargePayloadSupportEnabled() {

        AmazonS3 s3 = mock(AmazonS3.class);
        when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setLargePayloadSupportEnabled(s3, s3BucketName);

        Assert.assertTrue(extendedClientConfiguration.isLargePayloadSupportEnabled());
        Assert.assertNotNull(extendedClientConfiguration.getAmazonS3Client());
        Assert.assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testDisableLargePayloadSupport() {

        AmazonS3 s3 = mock(AmazonS3.class);
        when(s3.putObject(isA(PutObjectRequest.class))).thenReturn(null);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setLargePayloadSupportDisabled();

        Assert.assertNull(extendedClientConfiguration.getAmazonS3Client());
        Assert.assertNull(extendedClientConfiguration.getS3BucketName());

        verify(s3, never()).putObject(isA(PutObjectRequest.class));
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
