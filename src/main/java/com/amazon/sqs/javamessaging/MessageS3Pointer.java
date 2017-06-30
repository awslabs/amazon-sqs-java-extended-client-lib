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

/**
 * This class is used for carrying pointer to Amazon S3 objects which contain
 * message payloads. For a large-payload messages, an instance of this class
 * will be serialized to JSON and sent through Amazon SQS.
 * 
 */
class MessageS3Pointer {
	private String s3BucketName;
	private String s3Key;

	public MessageS3Pointer() {
	}

	public MessageS3Pointer(String s3BucketName, String s3Key) {
		this.s3BucketName = s3BucketName;
		this.s3Key = s3Key;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setS3BucketName(String s3BucketName) {
		this.s3BucketName = s3BucketName;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(String s3Key) {
		this.s3Key = s3Key;
	}

}
