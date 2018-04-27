/*
 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * Defines the contract of a S3 object key generator to use when persisting
 * SQS messages to an S3 bucket.
 *
 * Your implementation must always return a unique key, even if it is passed
 * the same SendMessageRequest or SendMessageBatchRequestEntry instance.
 */
public interface S3KeyGenerator {
    /**
     * Method to generate a unique S3 object key from a SendMessageRequest instance.
     *
     * @param sendMessageRequest the request object for the new message.
     * @return a unique S3 object key.
     */
    String generateObjectKey(SendMessageRequest sendMessageRequest);

    /**
     * Method to generate a unique S3 object key from a SendMessageBatchRequestEntry
     * instance.
     *
     * @param batchEntry the batch request object for the new message.
     * @return a unique S3 object key.
     */
    String generateObjectKey(SendMessageBatchRequestEntry batchEntry);
}
