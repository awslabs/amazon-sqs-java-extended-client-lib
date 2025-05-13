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


import java.util.regex.Pattern;

public class SQSExtendedClientConstants {
    // This constant is shared with SNSExtendedClient
    // SNS team should be notified of any changes made to this
    public static final String RESERVED_ATTRIBUTE_NAME = "ExtendedPayloadSize";

    // This constant is shared with SNSExtendedClient
    // SNS team should be notified of any changes made to this
    public static final int MAX_ALLOWED_ATTRIBUTES = 10 - 1; // 10 for SQS, 1 for the reserved attribute

    // This constant is shared with SNSExtendedClient
    // SNS team should be notified of any changes made to this
    public static final int DEFAULT_MESSAGE_SIZE_THRESHOLD = 262144;

    public static final String S3_BUCKET_NAME_MARKER = "-..s3BucketName..-";
    public static final String S3_KEY_MARKER = "-..s3Key..-";

    public static final int UUID_LENGTH = 36;

    public static final int MAX_S3_KEY_LENGTH = 1024;

    public static final int MAX_S3_KEY_PREFIX_LENGTH = MAX_S3_KEY_LENGTH - UUID_LENGTH;

    public static final Pattern INVALID_S3_PREFIX_KEY_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9./_-]");
}
