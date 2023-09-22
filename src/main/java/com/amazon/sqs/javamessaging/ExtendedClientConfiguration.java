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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.payloadoffloading.PayloadStorageConfiguration;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

import java.util.regex.Pattern;


/**
 * Amazon SQS extended client configuration options such as Amazon S3 client,
 * bucket name, and message size threshold for large-payload messages.
 */
@NotThreadSafe
public class ExtendedClientConfiguration extends PayloadStorageConfiguration {
    private static final Log LOG = LogFactory.getLog(ExtendedClientConfiguration.class);

    private static final int UUID_LENGTH = 36;
    private static final int MAX_S3_KEY_LENGTH = 1024;
    private static final int MAX_S3_KEY_PREFIX_LENGTH = MAX_S3_KEY_LENGTH - UUID_LENGTH;
    private static final Pattern INVALID_S3_PREFIX_KEY_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9./_-]");

    private boolean cleanupS3Payload = true;
    private boolean useLegacyReservedAttributeName = true;
    private boolean ignorePayloadNotFound = false;
    private String s3KeyPrefix = "";

    public ExtendedClientConfiguration() {
        super();
        this.setPayloadSizeThreshold(SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD);
    }

    public ExtendedClientConfiguration(ExtendedClientConfiguration other) {
        super(other);
        this.cleanupS3Payload = other.doesCleanupS3Payload();
        this.useLegacyReservedAttributeName = other.usesLegacyReservedAttributeName();
        this.ignorePayloadNotFound = other.ignoresPayloadNotFound();
        this.s3KeyPrefix = other.s3KeyPrefix;
    }

    /**
     * Enables support for payload messages.
     * @param s3
     *            Amazon S3 client which is going to be used for storing
     *            payload messages.
     * @param s3BucketName
     *            Name of the bucket which is going to be used for storing
     *            payload messages. The bucket must be already created and
     *            configured in s3.
     * @param cleanupS3Payload
     *            If set to true, would handle deleting the S3 object as part
     *            of deleting the message from SQS queue. Otherwise, would not
     *            attempt to delete the object from S3. If opted to not delete S3
     *            objects its the responsibility to the message producer to handle
     *            the clean up appropriately.
     */
    public void setPayloadSupportEnabled(S3Client s3, String s3BucketName, boolean cleanupS3Payload) {
        setPayloadSupportEnabled(s3, s3BucketName);
        this.cleanupS3Payload = cleanupS3Payload;
    }

    /**
     * Enables support for payload messages.
     * @param s3
     *            Amazon S3 client which is going to be used for storing
     *            payload messages.
     * @param s3BucketName
     *            Name of the bucket which is going to be used for storing
     *            payload messages. The bucket must be already created and
     *            configured in s3.
     * @param cleanupS3Payload
     *            If set to true, would handle deleting the S3 object as part
     *            of deleting the message from SQS queue. Otherwise, would not
     *            attempt to delete the object from S3. If opted to not delete S3
     *            objects its the responsibility to the message producer to handle
     *            the clean up appropriately.
     */
    public ExtendedClientConfiguration withPayloadSupportEnabled(S3Client s3, String s3BucketName, boolean cleanupS3Payload) {
        setPayloadSupportEnabled(s3, s3BucketName, cleanupS3Payload);
        return this;
    }

    /**
     * Disables the utilization legacy payload attribute name when sending messages.
     */
    public void setLegacyReservedAttributeNameDisabled() {
        this.useLegacyReservedAttributeName = false;
    }

    /**
     * Disables the utilization legacy payload attribute name when sending messages.
     */
    public ExtendedClientConfiguration withLegacyReservedAttributeNameDisabled() {
        setLegacyReservedAttributeNameDisabled();
        return this;
    }

    /**
     * Sets whether or not messages should be removed from Amazon SQS
     * when payloads are not found in Amazon S3.
     *
     * @param ignorePayloadNotFound
     *            Whether or not messages should be removed from Amazon SQS
     *            when payloads are not found in Amazon S3. Default: false
     */
    public void setIgnorePayloadNotFound(boolean ignorePayloadNotFound) {
        this.ignorePayloadNotFound = ignorePayloadNotFound;
    }

    /**
     * Sets whether or not messages should be removed from Amazon SQS
     * when payloads are not found in Amazon S3.
     *
     * @param ignorePayloadNotFound
     *            Whether or not messages should be removed from Amazon SQS
     *            when payloads are not found in Amazon S3. Default: false
     * @return the updated ExtendedClientConfiguration object.
     */
    public ExtendedClientConfiguration withIgnorePayloadNotFound(boolean ignorePayloadNotFound) {
        setIgnorePayloadNotFound(ignorePayloadNotFound);
        return this;
    }

    /**
     * Sets a string that will be used as prefix of the S3 Key.
     *
     * @param s3KeyPrefix
     *         A S3 key prefix value
     */
    public void setS3KeyPrefix(String s3KeyPrefix) {
        String trimmedPrefix = StringUtils.trimToEmpty(s3KeyPrefix);

        if (trimmedPrefix.length() > MAX_S3_KEY_PREFIX_LENGTH) {
            String errorMessage = "The S3 key prefix length must not be greater than " + MAX_S3_KEY_PREFIX_LENGTH;
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        if (trimmedPrefix.startsWith(".") || trimmedPrefix.startsWith("/")) {
            String errorMessage = "The S3 key prefix must not starts with '.' or '/'";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        if (trimmedPrefix.contains("..")) {
            String errorMessage = "The S3 key prefix must not contains the string '..'";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        if (INVALID_S3_PREFIX_KEY_CHARACTERS_PATTERN.matcher(trimmedPrefix).find()) {
            String errorMessage = "The S3 key prefix contain invalid characters. The allowed characters are: letters, digits, '/', '_', '-', and '.'";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        this.s3KeyPrefix = trimmedPrefix;
    }

    /**
     * Sets a string that will be used as prefix of the S3 Key.
     *
     * @param s3KeyPrefix
     *         A S3 key prefix value
     *
     * @return the updated ExtendedClientConfiguration object.
     */
    public ExtendedClientConfiguration withS3KeyPrefix(String s3KeyPrefix) {
        setS3KeyPrefix(s3KeyPrefix);
        return this;
    }

    /**
     * Gets the S3 key prefix
     * @return the prefix value which is being used for compose the S3 key.
     */
    public String getS3KeyPrefix() {
        return this.s3KeyPrefix;
    }

    /**
     * Checks whether or not clean up large objects in S3 is enabled.
     *
     * @return True if clean up is enabled when deleting the concerning SQS message.
     *         Default: true
     */
    public boolean doesCleanupS3Payload() {
        return cleanupS3Payload;
    }

    /**
     * Checks whether or not the configuration uses the legacy reserved attribute name.
     *
     * @return True if legacy reserved attribute name is used.
     *         Default: true
     */

    public boolean usesLegacyReservedAttributeName() {
        return useLegacyReservedAttributeName;
    }

    /**
     * Checks whether or not messages should be removed from Amazon SQS
     * when payloads are not found in Amazon S3.
     *
     * @return True if messages should be removed from Amazon SQS
     *         when payloads are not found in Amazon S3. Default: false
     */
    public boolean ignoresPayloadNotFound() {
        return ignorePayloadNotFound;
    }

    @Override
    public ExtendedClientConfiguration withAlwaysThroughS3(boolean alwaysThroughS3) {
        setAlwaysThroughS3(alwaysThroughS3);
        return this;
    }

    @Override
    public ExtendedClientConfiguration withPayloadSupportEnabled(S3Client s3, String s3BucketName) {
        this.setPayloadSupportEnabled(s3, s3BucketName);
        return this;
    }

    @Override
    public ExtendedClientConfiguration withObjectCannedACL(ObjectCannedACL objectCannedACL) {
        this.setObjectCannedACL(objectCannedACL);
        return this;
    }

    @Override
    public ExtendedClientConfiguration withPayloadSizeThreshold(int payloadSizeThreshold) {
        this.setPayloadSizeThreshold(payloadSizeThreshold);
        return this;
    }

    @Override
    public ExtendedClientConfiguration withPayloadSupportDisabled() {
        this.setPayloadSupportDisabled();
        return this;
    }

    @Override
    public ExtendedClientConfiguration withServerSideEncryption(ServerSideEncryptionStrategy serverSideEncryption) {
        this.setServerSideEncryptionStrategy(serverSideEncryption);
        return this;
    }

    /**
     * Enables support for large-payload messages.
     *
     * @param s3
     *            Amazon S3 client which is going to be used for storing
     *            large-payload messages.
     * @param s3BucketName
     *            Name of the bucket which is going to be used for storing
     *            large-payload messages. The bucket must be already created and
     *            configured in s3.
     *
     * @deprecated Instead use {@link #setPayloadSupportEnabled(S3Client, String, boolean)}
     */
    @Deprecated
    public void setLargePayloadSupportEnabled(S3Client s3, String s3BucketName) {
        this.setPayloadSupportEnabled(s3, s3BucketName);
    }

    /**
     * Enables support for large-payload messages.
     *
     * @param s3
     *            Amazon S3 client which is going to be used for storing
     *            large-payload messages.
     * @param s3BucketName
     *            Name of the bucket which is going to be used for storing
     *            large-payload messages. The bucket must be already created and
     *            configured in s3.
     * @return the updated ExtendedClientConfiguration object.
     *
     * @deprecated Instead use {@link #withPayloadSupportEnabled(S3Client, String)}
     */
    @Deprecated
    public ExtendedClientConfiguration withLargePayloadSupportEnabled(S3Client s3, String s3BucketName) {
        setLargePayloadSupportEnabled(s3, s3BucketName);
        return this;
    }

    /**
     * Disables support for large-payload messages.
     *
     * @deprecated Instead use {@link #setPayloadSupportDisabled()}
     */
    @Deprecated
    public void setLargePayloadSupportDisabled() {
        this.setPayloadSupportDisabled();
    }

    /**
     * Disables support for large-payload messages.
     * @return the updated ExtendedClientConfiguration object.
     *
     * @deprecated Instead use {@link #withPayloadSupportDisabled()}
     */
    @Deprecated
    public ExtendedClientConfiguration withLargePayloadSupportDisabled() {
        setLargePayloadSupportDisabled();
        return this;
    }

    /**
     * Check if the support for large-payload message if enabled.
     * @return true if support for large-payload messages is enabled.
     *
     * @deprecated Instead use {@link #isPayloadSupportEnabled()}
     */
    @Deprecated
    public boolean isLargePayloadSupportEnabled() {
        return isPayloadSupportEnabled();
    }

    /**
     * Sets the message size threshold for storing message payloads in Amazon
     * S3.
     *
     * @param messageSizeThreshold
     *            Message size threshold to be used for storing in Amazon S3.
     *            Default: 256KB.
     *
     * @deprecated Instead use {@link #setPayloadSizeThreshold(int)}
     */
    @Deprecated
    public void setMessageSizeThreshold(int messageSizeThreshold) {
        this.setPayloadSizeThreshold(messageSizeThreshold);
    }

    /**
     * Sets the message size threshold for storing message payloads in Amazon
     * S3.
     *
     * @param messageSizeThreshold
     *            Message size threshold to be used for storing in Amazon S3.
     *            Default: 256KB.
     * @return the updated ExtendedClientConfiguration object.
     *
     * @deprecated Instead use {@link #withPayloadSizeThreshold(int)}
     */
    @Deprecated
    public ExtendedClientConfiguration withMessageSizeThreshold(int messageSizeThreshold) {
        setMessageSizeThreshold(messageSizeThreshold);
        return this;
    }

    /**
     * Gets the message size threshold for storing message payloads in Amazon
     * S3.
     *
     * @return Message size threshold which is being used for storing in Amazon
     *         S3. Default: 256KB.
     *
     * @deprecated Instead use {@link #getPayloadSizeThreshold()}
     */
    @Deprecated
    public int getMessageSizeThreshold() {
        return getPayloadSizeThreshold();
    }
}