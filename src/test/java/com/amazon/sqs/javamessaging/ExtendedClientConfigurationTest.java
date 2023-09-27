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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.payloadoffloading.ServerSideEncryptionFactory;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;


/**
 * Tests the ExtendedClientConfiguration class.
 */
public class ExtendedClientConfigurationTest {

    private static final String s3BucketName = "test-bucket-name";
    private static final String s3ServerSideEncryptionKMSKeyId = "test-customer-managed-kms-key-id";
    private static final ServerSideEncryptionStrategy serverSideEncryptionStrategy = ServerSideEncryptionFactory.customerKey(s3ServerSideEncryptionKMSKeyId);

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

        assertEquals(s3, newExtendedClientConfig.getS3Client());
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
        S3Client s3 = mock(S3Client.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName);

        assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        assertNotNull(extendedClientConfiguration.getS3Client());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Enabled() {

        S3Client s3 = mock(S3Client.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, true);

        assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        assertTrue(extendedClientConfiguration.doesCleanupS3Payload());
        assertNotNull(extendedClientConfiguration.getS3Client());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testLargePayloadSupportEnabledWithDeleteFromS3Disabled() {
        S3Client s3 = mock(S3Client.class);
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName, false);

        assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        assertFalse(extendedClientConfiguration.doesCleanupS3Payload());
        assertNotNull(extendedClientConfiguration.getS3Client());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testCopyConstructorDeprecated() {
        S3Client s3 = mock(S3Client.class);

        boolean alwaysThroughS3 = true;
        int messageSizeThreshold = 500;

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration();

        extendedClientConfig.withPayloadSupportEnabled(s3, s3BucketName)
                .withAlwaysThroughS3(alwaysThroughS3).withPayloadSizeThreshold(messageSizeThreshold);

        ExtendedClientConfiguration newExtendedClientConfig = new ExtendedClientConfiguration(extendedClientConfig);

        assertEquals(s3, newExtendedClientConfig.getS3Client());
        assertEquals(s3BucketName, newExtendedClientConfig.getS3BucketName());
        assertTrue(newExtendedClientConfig.isPayloadSupportEnabled());
        assertEquals(alwaysThroughS3, newExtendedClientConfig.isAlwaysThroughS3());
        assertEquals(messageSizeThreshold, newExtendedClientConfig.getPayloadSizeThreshold());

        assertNotSame(newExtendedClientConfig, extendedClientConfig);
    }

    @Test
    public void testLargePayloadSupportEnabled() {

        S3Client s3 = mock(S3Client.class);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportEnabled(s3, s3BucketName);

        assertTrue(extendedClientConfiguration.isPayloadSupportEnabled());
        assertNotNull(extendedClientConfiguration.getS3Client());
        assertEquals(s3BucketName, extendedClientConfiguration.getS3BucketName());

    }

    @Test
    public void testDisableLargePayloadSupport() {
        S3Client s3 = mock(S3Client.class);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
        extendedClientConfiguration.setPayloadSupportDisabled();

        assertNull(extendedClientConfiguration.getS3Client());
        assertNull(extendedClientConfiguration.getS3BucketName());
    }

    @Test
    public void testMessageSizeThreshold() {

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        assertEquals(SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD,
                extendedClientConfiguration.getPayloadSizeThreshold());

        int messageLength = 1000;
        extendedClientConfiguration.setPayloadSizeThreshold(messageLength);
        assertEquals(messageLength, extendedClientConfiguration.getPayloadSizeThreshold());

    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test-s3-key-prefix",
        "TEST-S3-KEY-PREFIX",
        "test.s3.key.prefix",
        "test_s3_key_prefix",
        "test/s3/key/prefix/"
    })
    public void testS3keyPrefix(String s3KeyPrefix) {
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        extendedClientConfiguration.withS3KeyPrefix(s3KeyPrefix);

        assertEquals(s3KeyPrefix, extendedClientConfiguration.getS3KeyPrefix());
    }

    @Test
    public void testTrimS3keyPrefix() {
        String s3KeyPrefix = "test-s3-key-prefix";
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        extendedClientConfiguration.withS3KeyPrefix(String.format("   %s  ", s3KeyPrefix));

        assertEquals(s3KeyPrefix, extendedClientConfiguration.getS3KeyPrefix());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ".test-s3-key-prefix",
        "./test-s3-key-prefix",
        "../test-s3-key-prefix",
        "/test-s3-key-prefix",
        "test..s3..key..prefix",
        "test-s3-key-prefix@",
        "test s3 key prefix"
    })
    public void testS3KeyPrefixWithInvalidCharacters(String s3KeyPrefix) {
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        assertThrows(SdkClientException.class, () -> extendedClientConfiguration.withS3KeyPrefix(s3KeyPrefix));
    }

    @Test
    public void testS3keyPrefixWithALargeString() {
        int maxS3KeyLength = 1024;
        int uuidLength = 36;
        int maxS3KeyPrefixLength = maxS3KeyLength - uuidLength;
        String s3KeyPrefix = generateStringWithLength(maxS3KeyPrefixLength + 1);

        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();

        assertThrows(SdkClientException.class, () -> extendedClientConfiguration.withS3KeyPrefix(s3KeyPrefix));
    }
}
