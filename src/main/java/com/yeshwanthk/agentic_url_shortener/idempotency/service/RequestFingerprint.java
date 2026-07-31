package com.yeshwanthk.agentic_url_shortener.idempotency.service;

import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RequestFingerprint {

    public String create(CreateShortUrlRequest request) {
        String canonicalRequest = request.url().trim()
                + "\n"
                + (request.expiresAt() == null
                ? ""
                : request.expiresAt().toString());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            canonicalRequest.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not supported by this JVM",
                    exception
            );
        }
    }
}
