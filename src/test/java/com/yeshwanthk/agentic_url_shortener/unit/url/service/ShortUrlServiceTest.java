package com.yeshwanthk.agentic_url_shortener.unit.url.service;

import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortCodeGenerator;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import com.yeshwanthk.agentic_url_shortener.url.exception.InvalidUrlException;
import com.yeshwanthk.agentic_url_shortener.url.exception.ShortUrlNotFoundException;
import com.yeshwanthk.agentic_url_shortener.url.repository.ShortUrlRepository;
import com.yeshwanthk.agentic_url_shortener.url.service.ShortUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-31T12:00:00Z");

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        service = new ShortUrlService(
                repository,
                shortCodeGenerator,
                clock,
                "https://sho.rt/"
        );
    }

    @Test
    void createsShortUrlWithNormalizedUrl() {
        when(shortCodeGenerator.generate()).thenReturn("Ab12Cd34");
        when(repository.existsByShortCode("Ab12Cd34")).thenReturn(false);
        when(repository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new CreateShortUrlRequest(
                "HTTPS://Example.COM:443/path/../resource?x=1",
                NOW.plusSeconds(3600)
        ));

        assertThat(response.shortCode()).isEqualTo("Ab12Cd34");
        assertThat(response.shortUrl()).isEqualTo("https://sho.rt/Ab12Cd34");
        assertThat(response.originalUrl())
                .isEqualTo("HTTPS://Example.COM:443/path/../resource?x=1");
    }

    @Test
    void rejectsUnsupportedUrlScheme() {
        var request = new CreateShortUrlRequest(
                "ftp://example.com/file.txt",
                null
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessage("Only HTTP and HTTPS URLs are supported");
    }

    @Test
    void rejectsUrlContainingCredentials() {
        var request = new CreateShortUrlRequest(
                "https://username:password@example.com/private",
                null
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessage(
                        "URLs containing embedded credentials are not supported"
                );
    }

    @Test
    void resolvesActiveShortUrl() {
        ShortUrl shortUrl = ShortUrl.create(
                "Ab12Cd34",
                "https://example.com/resource",
                "https://example.com/resource",
                NOW.minusSeconds(60),
                null
        );

        when(repository.findByShortCode("Ab12Cd34"))
                .thenReturn(Optional.of(shortUrl));

        assertThat(service.resolve("Ab12Cd34"))
                .hasToString("https://example.com/resource");
    }

    @Test
    void rejectsExpiredShortUrl() {
        ShortUrl expired = ShortUrl.create(
                "Ab12Cd34",
                "https://example.com",
                "https://example.com/",
                NOW.minusSeconds(120),
                NOW.minusSeconds(60)
        );

        when(repository.findByShortCode("Ab12Cd34"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolve("Ab12Cd34"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }
}