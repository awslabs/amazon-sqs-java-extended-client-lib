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
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.annotation.NotThreadSafe;
import software.amazon.payloadoffloading.PayloadStorageConfiguration;


/**
 * Amazon SQS extended client configuration options such as Amazon S3 client,
 * bucket name, and message size threshold for large-payload messages.
 */
@NotThreadSafe
public class ExtendedClientConfiguration extends PayloadStorageConfiguration {

    private boolean cleanupS3Payload = true;
    private boolean useLegacyReservedAttributeName = true;

    public ExtendedClientConfiguration() {
        super();
        this.setPayloadSizeThreshold(SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD);
    }

    public ExtendedClientConfiguration(ExtendedClientConfiguration other) {
        super(other);
        this.cleanupS3Payload = other.doesCleanupS3Payload();
        this.useLegacyReservedAttributeName = other.usesLegacyReservedAttributeName();
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
    public void setPayloadSupportEnabled(AmazonS3 s3, String s3BucketName, boolean cleanupS3Payload) {
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
    public ExtendedClientConfiguration withPayloadSupportEnabled(AmazonS3 s3, String s3BucketName, boolean cleanupS3Payload) {
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

    @Override
    public ExtendedClientConfiguration withAlwaysThroughS3(boolean alwaysThroughS3) {
        setAlwaysThroughS3(alwaysThroughS3);
        return this;
    }

    @Override
    public ExtendedClientConfiguration withPayloadSupportEnabled(AmazonS3 s3, String s3BucketName) {
        this.setPayloadSupportEnabled(s3, s3BucketName);
        return this;
    }

    @Override
    public ExtendedClientConfiguration withSSEAwsKeyManagementParams(SSEAwsKeyManagementParams sseAwsKeyManagementParams) {
        this.setSSEAwsKeyManagementParams(sseAwsKeyManagementParams);
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
}