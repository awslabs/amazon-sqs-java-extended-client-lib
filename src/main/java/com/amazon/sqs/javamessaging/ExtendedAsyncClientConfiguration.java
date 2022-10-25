package com.amazon.sqs.javamessaging;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.payloadoffloading.PayloadStorageAsyncConfiguration;
import software.amazon.payloadoffloading.ServerSideEncryptionStrategy;

/**
 * Amazon SQS extended client configuration options such as async Amazon S3 client,
 * bucket name, and message size threshold for large-payload messages.
 */
@NotThreadSafe
public class ExtendedAsyncClientConfiguration extends PayloadStorageAsyncConfiguration {

    private boolean cleanupS3Payload = true;
    private boolean useLegacyReservedAttributeName = true;
    private boolean ignorePayloadNotFound = false;

    public ExtendedAsyncClientConfiguration() {
        this.setPayloadSizeThreshold(SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD);
    }

    public ExtendedAsyncClientConfiguration(ExtendedAsyncClientConfiguration other) {
        super(other);
        this.cleanupS3Payload = other.doesCleanupS3Payload();
        this.useLegacyReservedAttributeName = other.usesLegacyReservedAttributeName();
        this.ignorePayloadNotFound = other.ignoresPayloadNotFound();
    }

    /**
     * Enables asynchronous support for payload messages.
     * @param s3Async
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
    public void setPayloadSupportEnabled(S3AsyncClient s3Async, String s3BucketName, boolean cleanupS3Payload) {
        setPayloadSupportEnabled(s3Async, s3BucketName);
        this.cleanupS3Payload = cleanupS3Payload;
    }

    /**
     * Enables asynchronous support for payload messages.
     * @param s3Async
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
    public ExtendedAsyncClientConfiguration withPayloadSupportEnabled(
        S3AsyncClient s3Async, String s3BucketName, boolean cleanupS3Payload) {
        setPayloadSupportEnabled(s3Async, s3BucketName, cleanupS3Payload);
        return this;
    }

    @Override
    public ExtendedAsyncClientConfiguration withPayloadSupportEnabled(S3AsyncClient s3Async, String s3BucketName) {
        this.setPayloadSupportEnabled(s3Async, s3BucketName);
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
    public ExtendedAsyncClientConfiguration withLegacyReservedAttributeNameDisabled() {
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
     * @return the updated ExtendedAsyncClientConfiguration object.
     */
    public ExtendedAsyncClientConfiguration withIgnorePayloadNotFound(boolean ignorePayloadNotFound) {
        setIgnorePayloadNotFound(ignorePayloadNotFound);
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
    public ExtendedAsyncClientConfiguration withAlwaysThroughS3(boolean alwaysThroughS3) {
        setAlwaysThroughS3(alwaysThroughS3);
        return this;
    }

    @Override
    public ExtendedAsyncClientConfiguration withObjectCannedACL(ObjectCannedACL objectCannedACL) {
        this.setObjectCannedACL(objectCannedACL);
        return this;
    }

    @Override
    public ExtendedAsyncClientConfiguration withPayloadSizeThreshold(int payloadSizeThreshold) {
        this.setPayloadSizeThreshold(payloadSizeThreshold);
        return this;
    }

    @Override
    public ExtendedAsyncClientConfiguration withPayloadSupportDisabled() {
        this.setPayloadSupportDisabled();
        return this;
    }

    @Override
    public ExtendedAsyncClientConfiguration withServerSideEncryption(ServerSideEncryptionStrategy serverSideEncryption) {
        this.setServerSideEncryptionStrategy(serverSideEncryption);
        return this;
    }
}