package com.amazon.sqs.javamessaging;

import java.util.UUID;

/**
 * Class for generating keys for storing messages to Amazon S3.
 */
public class KeyGenerator {

    public static final KeyGenerator DEFAULT_KEY_GENERATOR = new KeyGenerator();

    /**
     * Generates a unique for use when storing to Amazon S3.
     *
     * @return a unique key for writing to S3.
     */
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
