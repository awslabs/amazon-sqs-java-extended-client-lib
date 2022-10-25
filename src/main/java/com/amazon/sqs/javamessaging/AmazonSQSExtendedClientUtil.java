package com.amazon.sqs.javamessaging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.payloadoffloading.PayloadS3Pointer;
import software.amazon.payloadoffloading.Util;

public class AmazonSQSExtendedClientUtil {
    private static final Log LOG = LogFactory.getLog(AmazonSQSExtendedClientUtil.class);

    public static final String LEGACY_RESERVED_ATTRIBUTE_NAME = "SQSLargePayloadSize";
    public static final List<String> RESERVED_ATTRIBUTE_NAMES = Arrays.asList(LEGACY_RESERVED_ATTRIBUTE_NAME,
        SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME);

    public static void checkMessageAttributes(int payloadSizeThreshold, Map<String, MessageAttributeValue> messageAttributes) {
        int msgAttributesSize = getMsgAttributesSize(messageAttributes);
        if (msgAttributesSize > payloadSizeThreshold) {
            String errorMessage = "Total size of Message attributes is " + msgAttributesSize
                                  + " bytes which is larger than the threshold of " + payloadSizeThreshold
                                  + " Bytes. Consider including the payload in the message body instead of message attributes.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }

        int messageAttributesNum = messageAttributes.size();
        if (messageAttributesNum > SQSExtendedClientConstants.MAX_ALLOWED_ATTRIBUTES) {
            String errorMessage = "Number of message attributes [" + messageAttributesNum
                                  + "] exceeds the maximum allowed for large-payload messages ["
                                  + SQSExtendedClientConstants.MAX_ALLOWED_ATTRIBUTES + "].";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }
        Optional<String> largePayloadAttributeName = getReservedAttributeNameIfPresent(messageAttributes);

        if (largePayloadAttributeName.isPresent()) {
            String errorMessage = "Message attribute name " + largePayloadAttributeName.get()
                                  + " is reserved for use by SQS extended client.";
            LOG.error(errorMessage);
            throw SdkClientException.create(errorMessage);
        }
    }

    public static Optional<String> getReservedAttributeNameIfPresent(Map<String, MessageAttributeValue> msgAttributes) {
        String reservedAttributeName = null;
        if (msgAttributes.containsKey(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME)) {
            reservedAttributeName = SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME;
        } else if (msgAttributes.containsKey(LEGACY_RESERVED_ATTRIBUTE_NAME)) {
            reservedAttributeName = LEGACY_RESERVED_ATTRIBUTE_NAME;
        }
        return Optional.ofNullable(reservedAttributeName);
    }

    public static String embedS3PointerInReceiptHandle(String receiptHandle, String pointer) {
        PayloadS3Pointer s3Pointer = PayloadS3Pointer.fromJson(pointer);
        String s3MsgBucketName = s3Pointer.getS3BucketName();
        String s3MsgKey = s3Pointer.getS3Key();

        return SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + s3MsgBucketName
               + SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER + SQSExtendedClientConstants.S3_KEY_MARKER
               + s3MsgKey + SQSExtendedClientConstants.S3_KEY_MARKER + receiptHandle;
    }

    public static String getOrigReceiptHandle(String receiptHandle) {
        int secondOccurence = receiptHandle.indexOf(SQSExtendedClientConstants.S3_KEY_MARKER,
            receiptHandle.indexOf(SQSExtendedClientConstants.S3_KEY_MARKER) + 1);
        return receiptHandle.substring(secondOccurence + SQSExtendedClientConstants.S3_KEY_MARKER.length());
    }

    public static boolean isS3ReceiptHandle(String receiptHandle) {
        return receiptHandle.contains(SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER)
               && receiptHandle.contains(SQSExtendedClientConstants.S3_KEY_MARKER);
    }

    public static String getMessagePointerFromModifiedReceiptHandle(String receiptHandle) {
        String s3MsgBucketName = getFromReceiptHandleByMarker(
            receiptHandle, SQSExtendedClientConstants.S3_BUCKET_NAME_MARKER);
        String s3MsgKey = getFromReceiptHandleByMarker(receiptHandle, SQSExtendedClientConstants.S3_KEY_MARKER);

        PayloadS3Pointer payloadS3Pointer = new PayloadS3Pointer(s3MsgBucketName, s3MsgKey);
        return payloadS3Pointer.toJson();
    }

    public static boolean isLarge(int payloadSizeThreshold, SendMessageRequest sendMessageRequest) {
        int msgAttributesSize = getMsgAttributesSize(sendMessageRequest.messageAttributes());
        long msgBodySize = Util.getStringSizeInBytes(sendMessageRequest.messageBody());
        long totalMsgSize = msgAttributesSize + msgBodySize;
        return (totalMsgSize > payloadSizeThreshold);
    }

    public static boolean isLarge(int payloadSizeThreshold, SendMessageBatchRequestEntry batchEntry) {
        int msgAttributesSize = getMsgAttributesSize(batchEntry.messageAttributes());
        long msgBodySize = Util.getStringSizeInBytes(batchEntry.messageBody());
        long totalMsgSize = msgAttributesSize + msgBodySize;
        return (totalMsgSize > payloadSizeThreshold);
    }

    public static Map<String, MessageAttributeValue> updateMessageAttributePayloadSize(
        Map<String, MessageAttributeValue> messageAttributes, Long messageContentSize,
        boolean usesLegacyReservedAttributeName) {
        Map<String, MessageAttributeValue> updatedMessageAttributes = new HashMap<>(messageAttributes);

        // Add a new message attribute as a flag
        MessageAttributeValue.Builder messageAttributeValueBuilder = MessageAttributeValue.builder();
        messageAttributeValueBuilder.dataType("Number");
        messageAttributeValueBuilder.stringValue(messageContentSize.toString());
        MessageAttributeValue messageAttributeValue = messageAttributeValueBuilder.build();

        if (!usesLegacyReservedAttributeName) {
            updatedMessageAttributes.put(SQSExtendedClientConstants.RESERVED_ATTRIBUTE_NAME, messageAttributeValue);
        } else {
            updatedMessageAttributes.put(LEGACY_RESERVED_ATTRIBUTE_NAME, messageAttributeValue);
        }
        return updatedMessageAttributes;
    }

    @SuppressWarnings("unchecked")
    public static <T extends AwsRequest.Builder> T appendUserAgent(
        final T builder, String userAgentName, String userAgentVersion) {
        return (T) builder
            .overrideConfiguration(
                AwsRequestOverrideConfiguration.builder()
                    .addApiName(ApiName.builder().name(userAgentName)
                        .version(userAgentVersion).build())
                    .build());
    }

    private static String getFromReceiptHandleByMarker(String receiptHandle, String marker) {
        int firstOccurence = receiptHandle.indexOf(marker);
        int secondOccurence = receiptHandle.indexOf(marker, firstOccurence + 1);
        return receiptHandle.substring(firstOccurence + marker.length(), secondOccurence);
    }

    private static int getMsgAttributesSize(Map<String, MessageAttributeValue> msgAttributes) {
        int totalMsgAttributesSize = 0;
        for (Map.Entry<String, MessageAttributeValue> entry : msgAttributes.entrySet()) {
            totalMsgAttributesSize += Util.getStringSizeInBytes(entry.getKey());

            MessageAttributeValue entryVal = entry.getValue();
            if (entryVal.dataType() != null) {
                totalMsgAttributesSize += Util.getStringSizeInBytes(entryVal.dataType());
            }

            String stringVal = entryVal.stringValue();
            if (stringVal != null) {
                totalMsgAttributesSize += Util.getStringSizeInBytes(entryVal.stringValue());
            }

            SdkBytes binaryVal = entryVal.binaryValue();
            if (binaryVal != null) {
                totalMsgAttributesSize += binaryVal.asByteArray().length;
            }
        }
        return totalMsgAttributesSize;
    }
}
