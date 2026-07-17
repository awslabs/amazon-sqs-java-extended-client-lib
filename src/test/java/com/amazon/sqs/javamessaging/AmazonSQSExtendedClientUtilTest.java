package com.amazon.sqs.javamessaging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmazonSQSExtendedClientUtilTest {

    @Test
    public void testExtractMessageFromSnsJson() {
        String snsJson = "{\"Type\":\"Notification\",\"MessageId\":\"123\",\"Message\":\"Hello World\"}";
        assertEquals("Hello World", AmazonSQSExtendedClientUtil.extractMessageFromSnsJson(snsJson));
    }

    @Test
    public void testUpdateMessageInSnsJson() {
        String snsJson = "{\"Type\":\"Notification\",\"Subject\":\"Test\",\"Message\":\"Old\"}";
        String result = AmazonSQSExtendedClientUtil.updateMessageInSnsJson(snsJson, "NewContent");
        assertEquals("{\"Type\":\"Notification\",\"Subject\":\"Test\",\"Message\":\"NewContent\"}", result);
    }

    @Test
    public void testUpdateMessageInSnsJsonEscaping() {
        String snsJson = "{\"Message\":\"Old\"}";
        String result = AmazonSQSExtendedClientUtil.updateMessageInSnsJson(snsJson, "Line1\nLine2 \"Quoted\"");
        assertEquals("{\"Message\":\"Line1\\nLine2 \\\"Quoted\\\"\"}", result);
    }

    @Test
    public void testUpdateMessageInSnsJsonNonSnsJson() {
        // If it's valid JSON but doesn't have "Message", it should return the new message directly (fallback)
        String json = "{\"OtherField\":\"Value\"}";
        String result = AmazonSQSExtendedClientUtil.updateMessageInSnsJson(json, "NewContent");
        assertEquals("NewContent", result);
    }

    @Test
    public void testUpdateMessageInSnsJsonMalformed() {
        String malformed = "not json";
        String result = AmazonSQSExtendedClientUtil.updateMessageInSnsJson(malformed, "NewContent");
        assertEquals("NewContent", result);
    }
}
