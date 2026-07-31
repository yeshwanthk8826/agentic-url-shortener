package com.yeshwanthk.agentic_url_shortener.url.domain;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    static final int CODE_LENGTH = 8;

    private static final char[] BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                    .toCharArray();

    private final SecureRandom secureRandom;

    public ShortCodeGenerator() {
        this(new SecureRandom());
    }

    ShortCodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        char[] result = new char[CODE_LENGTH];

        for (int index = 0; index < CODE_LENGTH; index++) {
            result[index] = BASE62[secureRandom.nextInt(BASE62.length)];
        }

        return new String(result);
    }
}