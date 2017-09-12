/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.annotation.NotThreadSafe;

/**
 * Amazon SQS extended client configuration options such as Amazon S3 client,
 * bucket name, and message size threshold for large-payload messages.
 */
@NotThreadSafe
public class ExtendedClientConfiguration {
	private static final Log LOG = LogFactory.getLog(ExtendedClientConfiguration.class);

	private AmazonS3 s3;
	private String s3BucketName;
	private String s3Prefix;
	private boolean largePayloadSupport = false;
	private boolean alwaysThroughS3 = false;
	private int messageSizeThreshold = SQSExtendedClientConstants.DEFAULT_MESSAGE_SIZE_THRESHOLD;

	public ExtendedClientConfiguration() {
		s3 = null;
		s3BucketName = null;
		s3Prefix = null;
	}

	public ExtendedClientConfiguration(ExtendedClientConfiguration other) {
		this.s3 = other.s3;
		this.s3BucketName = other.s3BucketName;
		this.s3Prefix = other.s3Prefix;
		this.largePayloadSupport = other.largePayloadSupport;
		this.alwaysThroughS3 = other.alwaysThroughS3;
		this.messageSizeThreshold = other.messageSizeThreshold;
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
	 */
	public void setLargePayloadSupportEnabled(AmazonS3 s3, String s3BucketName) {
		setLargePayloadSupportEnabled(s3, s3BucketName, null);
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
	 * @param s3Prefix
	 *			  Optional custom s3 bucket prefix to use for large-payload messages. Needs to
	 *			  end with a '/'
	 */
	public void setLargePayloadSupportEnabled(AmazonS3 s3, String s3BucketName, String s3Prefix) {
		if (s3 == null || s3BucketName == null) {
			String errorMessage = "S3 client and/or S3 bucket name cannot be null.";
			LOG.error(errorMessage);
			throw new AmazonClientException(errorMessage);
		}
		if (isLargePayloadSupportEnabled()) {
			LOG.warn("Large-payload support is already enabled. Overwriting AmazonS3Client and S3BucketName.");
		}
		this.s3 = s3;
		this.s3BucketName = s3BucketName;
		this.s3Prefix = s3Prefix;
		largePayloadSupport = true;
		LOG.info("Large-payload support enabled.");
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
	 * @param s3Prefix
	 *			  Optional custom s3 bucket prefix to use for large-payload messages. Needs to
	 *			  end with a '/'.
	 * @return the updated ExtendedClientConfiguration object.
	 */
	public ExtendedClientConfiguration withLargePayloadSupportEnabled(AmazonS3 s3, String s3BucketName, String s3Prefix) {
		setLargePayloadSupportEnabled(s3, s3BucketName, s3Prefix);
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
	 * @return the updated ExtendedClientConfiguration object.
	 */
	public ExtendedClientConfiguration withLargePayloadSupportEnabled(AmazonS3 s3, String s3BucketName) {
		setLargePayloadSupportEnabled(s3, s3BucketName);
		return this;
	}

	/**
	 * Disables support for large-payload messages.
	 */
	public void setLargePayloadSupportDisabled() {
		s3 = null;
		s3BucketName = null;
		largePayloadSupport = false;
		LOG.info("Large-payload support disabled.");
	}

	/**
	 * Disables support for large-payload messages.
	 * @return the updated ExtendedClientConfiguration object.
	 */
	public ExtendedClientConfiguration withLargePayloadSupportDisabled() {
		setLargePayloadSupportDisabled();
		return this;
	}

	/**
	 * Check if the support for large-payload message if enabled.
	 *
	 * @return true if support for large-payload messages is enabled.
	 */
	public boolean isLargePayloadSupportEnabled() {
		return largePayloadSupport;
	}

	/**
	 * Gets the Amazon S3 client which is being used for storing large-payload
	 * messages.
	 *
	 * @return Reference to the Amazon S3 client which is being used.
	 */
	public AmazonS3 getAmazonS3Client() {
		return s3;
	}

	/**
	 * Gets the name of the S3 bucket which is being used for storing
	 * large-payload messages.
	 *
	 * @return The name of the bucket which is being used.
	 */
	public String getS3BucketName() {
		return s3BucketName;
	}

	/**
	 * Gets the name of the S3 bucket prefix which is being used for storing
	 * large-payload messages.
	 *
	 * @return The name of the bucket prefix which is being used.
	 */
	public String getS3Prefix() {
		return s3Prefix;
	}

	/**
	 * Sets the message size threshold for storing message payloads in Amazon
	 * S3.
	 *
	 * @param messageSizeThreshold
	 *            Message size threshold to be used for storing in Amazon S3.
	 *            Default: 256KB.
	 */
	public void setMessageSizeThreshold(int messageSizeThreshold) {
		this.messageSizeThreshold = messageSizeThreshold;
	}

	/**
	 * Sets the message size threshold for storing message payloads in Amazon
	 * S3.
	 *
	 * @param messageSizeThreshold
	 *            Message size threshold to be used for storing in Amazon S3.
	 *            Default: 256KB.
	 * @return the updated ExtendedClientConfiguration object.
	 */
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
	 */
	public int getMessageSizeThreshold() {
		return messageSizeThreshold;
	}

	/**
	 * Sets whether or not all messages regardless of their payload size should
	 * be stored in Amazon S3.
	 *
	 * @param alwaysThroughS3
	 *            Whether or not all messages regardless of their payload size
	 *            should be stored in Amazon S3. Default: false
	 */
	public void setAlwaysThroughS3(boolean alwaysThroughS3) {
		this.alwaysThroughS3 = alwaysThroughS3;
	}

	/**
	 * Sets whether or not all messages regardless of their payload size should
	 * be stored in Amazon S3.
	 *
	 * @param alwaysThroughS3
	 *            Whether or not all messages regardless of their payload size
	 *            should be stored in Amazon S3. Default: false
	 * @return the updated ExtendedClientConfiguration object.
	 */
	public ExtendedClientConfiguration withAlwaysThroughS3(boolean alwaysThroughS3) {
		setAlwaysThroughS3(alwaysThroughS3);
		return this;
	}

	/**
	 * Checks whether or not all messages regardless of their payload size are
	 * being stored in Amazon S3.
	 *
	 * @return True if all messages regardless of their payload size are being
	 *         stored in Amazon S3. Default: false
	 */
	public boolean isAlwaysThroughS3() {
		return alwaysThroughS3;
	}
}
