/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazon.sqs.javamessaging.SQSExtended;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;

public interface AmazonSQSExtended extends AmazonSQS {

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
	public void enableLargePayloadSupport(AmazonS3 s3, String s3BucketName);

	/**
	 * Disables support for large-payload messages.
	 */
	public void disableLargePayloadSupport();

	/**
	 * Check if the support for large-paylod message if enabled.
	 * 
	 * @return true if support for large-payload messages is enabled.
	 */
	public boolean hasLargePayloadSupport();

	/**
	 * Gets the Amazon S3 client which is being used for storing large-payload
	 * messages.
	 * 
	 * @return Reference to the Amazon S3 client which is being used.
	 */
	public AmazonS3 getAmazonS3Client();

	/**
	 * Gets the name of the S3 bucket which is being used for storing
	 * large-payload messages.
	 * 
	 * @return The name of the bucket which is being used.
	 */
	public String getS3BucketName();

	/**
	 * Sets the message size threshold for storing message-payloads in S3.
	 * 
	 * @param messageSizeThreshold
	 *            Message size threshold to be used for storing in S3. Default:
	 *            256KB.
	 */
	public void setMessageSizeThreshold(int messageSizeThreshold);

	/**
	 * Gets the message size threshold for storing message-payloads in S3.
	 * 
	 * @return Message size threshold which is being used for storing in S3.
	 *         Default: 256KB.
	 */
	public int getMessageSizeThreshold();

	/**
	 * Sets whether or not all messages regardless of their payload size should
	 * be stored in S3.
	 * 
	 * @param alwaysThroughS3
	 *            Whether or not all messages regardless of their payload size
	 *            should be stored in S3. Default: false
	 */
	public void setAlwaysThroughS3(boolean alwaysThroughS3);

	/**
	 * Checks whether or not all messages regardless of their payload size are
	 * being stored in S3.
	 * 
	 * @return True if all messages regardless of their payload size are being
	 *         stored in S3. Default: false
	 */
	public boolean isAlwaysThroughS3();

}
