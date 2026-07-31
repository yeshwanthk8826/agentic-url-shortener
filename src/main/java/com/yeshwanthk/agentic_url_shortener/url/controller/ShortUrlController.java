package com.yeshwanthk.agentic_url_shortener.url.controller;

import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import com.yeshwanthk.agentic_url_shortener.url.dto.ShortUrlResponse;
import com.yeshwanthk.agentic_url_shortener.url.service.ShortUrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RestController
@RequestMapping
public class ShortUrlController {

    private static final String SHORT_CODE_PATTERN = "^[0-9A-Za-z]{8}$";

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<ShortUrlResponse> create(
            @Valid @RequestBody CreateShortUrlRequest request
    ) {
        ShortUrlResponse response = shortUrlService.create(request);

        return ResponseEntity
                .created(URI.create(response.shortUrl()))
                .body(response);
    }

    @GetMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<ShortUrlResponse> get(
            @PathVariable
            @Pattern(
                    regexp = SHORT_CODE_PATTERN,
                    message = "Short code must contain exactly 8 Base62 characters"
            )
            String shortCode
    ) {
        return ResponseEntity.ok(shortUrlService.findByShortCode(shortCode));
    }

    @GetMapping("/{shortCode:" + SHORT_CODE_PATTERN + "}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        URI destination = shortUrlService.resolve(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(destination)
                .build();
    }
}
