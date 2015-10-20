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
package com.amazon.sqs.javamessaging;

class SQSExtendedClientConstants {

	public static final String MESSAGE_ATTRIB_NAME = "SQSLargePayloadSize";
	public static final int MAX_ALLOWED_ATTRIBUTES = 9;
	public static final String ERROR_MESSAGE_HEADER = "Error in Amazon SQS extended client.";
	public static final String WARNING_MESSAGE_HEADER = "Warning in Amazon SQS extended client.";
	public static final int DEFAULT_MESSAGE_SIZE_THRESH = 262144;

	// has '-' to avoid collision with Base64 receipt handles
	// has'..' to avoid collision with allowed bucket names
	public static final String S3_BUCKET_NAME_MARKER = "-..s3BucketName..-";
	public static final String S3_KEY_MARKER = "-..s3Key..-";

}
