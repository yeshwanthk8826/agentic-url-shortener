package com.yeshwanthk.agentic_url_shortener.unit.url.controller;

import com.yeshwanthk.agentic_url_shortener.config.SecurityConfig;
import com.yeshwanthk.agentic_url_shortener.idempotency.dto.IdempotentResult;
import com.yeshwanthk.agentic_url_shortener.idempotency.service.IdempotentUrlCreationService;
import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectCache;
import tools.jackson.databind.ObjectMapper;
import com.yeshwanthk.agentic_url_shortener.exception.ApiExceptionHandler;
import com.yeshwanthk.agentic_url_shortener.url.controller.ShortUrlController;
import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import com.yeshwanthk.agentic_url_shortener.url.dto.ShortUrlResponse;
import com.yeshwanthk.agentic_url_shortener.url.service.ShortUrlService;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrlStatus;
import com.yeshwanthk.agentic_url_shortener.url.exception.ShortUrlNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortUrlController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class
})
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShortUrlService shortUrlService;

    @MockitoBean
    private IdempotentUrlCreationService idempotentUrlCreationService;


    @Test
    void createsShortUrl() throws Exception {
        Instant createdAt = Instant.parse("2026-07-31T12:00:00Z");

        var response = new ShortUrlResponse(
                UUID.randomUUID(),
                "Ab12Cd34",
                "https://sho.rt/Ab12Cd34",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                createdAt,
                null
        );

        when(idempotentUrlCreationService.create(
                eq("request-123"),
                any(CreateShortUrlRequest.class)
        )).thenReturn(IdempotentResult.created(response));

        var request = new CreateShortUrlRequest(
                "https://example.com",
                null
        );

        mockMvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "request-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "https://sho.rt/Ab12Cd34"
                ))
                .andExpect(header().string(
                        "Idempotency-Replayed",
                        "false"
                ))
                .andExpect(jsonPath("$.shortCode").value("Ab12Cd34"))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://example.com"));
    }

    @Test
    void rejectsBlankUrl() throws Exception {
        var request = new CreateShortUrlRequest(" ", null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "request-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:problem:request-validation"))
                .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(idempotentUrlCreationService);
    }

    @Test
    void redirectsToOriginalUrl() throws Exception {
        when(shortUrlService.resolve("Ab12Cd34"))
                .thenReturn(URI.create("https://example.com/resource"));

        mockMvc.perform(get("/Ab12Cd34"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://example.com/resource"
                ));
    }

    @Test
    void returnsProblemDetailWhenShortUrlDoesNotExist() throws Exception {
        when(shortUrlService.resolve("Ab12Cd34"))
                .thenThrow(new ShortUrlNotFoundException("Ab12Cd34"));

        mockMvc.perform(get("/Ab12Cd34"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("urn:problem:short-url-not-found"))
                .andExpect(jsonPath("$.title")
                        .value("Short URL not found"));
    }

    @Test
    void replaysCompletedIdempotentRequest() throws Exception {
        Instant createdAt = Instant.parse("2026-07-31T12:00:00Z");

        var response = new ShortUrlResponse(
                UUID.randomUUID(),
                "Ab12Cd34",
                "https://sho.rt/Ab12Cd34",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                createdAt,
                null
        );

        when(idempotentUrlCreationService.create(
                eq("request-123"),
                any(CreateShortUrlRequest.class)
        )).thenReturn(IdempotentResult.replayed(response));

        var request = new CreateShortUrlRequest(
                "https://example.com",
                null
        );

        mockMvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "request-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Idempotency-Replayed",
                        "true"
                ))
                .andExpect(header().string(
                        "Location",
                        "https://sho.rt/Ab12Cd34"
                ))
                .andExpect(jsonPath("$.shortCode").value("Ab12Cd34"));
    }
}
