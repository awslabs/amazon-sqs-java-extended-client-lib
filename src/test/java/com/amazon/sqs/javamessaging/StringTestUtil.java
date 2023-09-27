package com.amazon.sqs.javamessaging;

import java.util.Arrays;

public class StringTestUtil {
    public static String generateStringWithLength(int messageLength) {
        char[] charArray = new char[messageLength];
        Arrays.fill(charArray, 'x');
        return new String(charArray);
    }
}
