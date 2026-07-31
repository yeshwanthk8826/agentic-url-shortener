package com.yeshwanthk.agentic_url_shortener.unit.url.service;

import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectCache;
import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectTarget;
import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortCodeGenerator;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import com.yeshwanthk.agentic_url_shortener.url.exception.InvalidUrlException;
import com.yeshwanthk.agentic_url_shortener.url.exception.ShortUrlNotFoundException;
import com.yeshwanthk.agentic_url_shortener.url.repository.ShortUrlRepository;
import com.yeshwanthk.agentic_url_shortener.url.service.ShortUrlService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;

import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-31T12:00:00Z");

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    private ShortUrlService service;
    @Mock
    private RedirectCache redirectCache;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        meterRegistry = new SimpleMeterRegistry();

        service = new ShortUrlService(
                repository,
                shortCodeGenerator,
                redirectCache,
                clock,
                meterRegistry,
                "https://sho.rt/"
        );
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
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

        verify(redirectCache).put(
                eq("Ab12Cd34"),
                any(RedirectTarget.class)
        );

        assertThat(
                meterRegistry.get("short_url.created")
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void rejectsRedirectWhenAtomicVisitUpdateDoesNotMatch() {
        RedirectTarget target = new RedirectTarget(
                "https://example.com/resource",
                com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrlStatus.ACTIVE,
                null
        );

        when(redirectCache.get(
                eq("Ab12Cd34"),
                any()
        )).thenReturn(target);

        when(repository.recordVisit("Ab12Cd34", NOW))
                .thenReturn(0);

        assertThatThrownBy(() -> service.resolve("Ab12Cd34"))
                .isInstanceOf(ShortUrlNotFoundException.class);

        verify(redirectCache).evict("Ab12Cd34");

        assertThat(
                meterRegistry.get("short_url.redirect")
                        .tag("outcome", "failure")
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void loadsRedirectTargetFromRepositoryOnCacheMiss() {
        ShortUrl shortUrl = ShortUrl.create(
                "Ab12Cd34",
                "https://example.com/resource",
                "https://example.com/resource",
                NOW.minusSeconds(60),
                null
        );

        when(redirectCache.get(
                eq("Ab12Cd34"),
                any()
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Function<String, RedirectTarget> loader =
                    invocation.getArgument(1);

            return loader.apply("Ab12Cd34");
        });

        when(repository.findByShortCode("Ab12Cd34"))
                .thenReturn(Optional.of(shortUrl));

        when(repository.recordVisit("Ab12Cd34", NOW))
                .thenReturn(1);

        assertThat(service.resolve("Ab12Cd34"))
                .hasToString("https://example.com/resource");

        verify(repository).findByShortCode("Ab12Cd34");
        verify(repository).recordVisit("Ab12Cd34", NOW);
    }

    @Test
    void rejectsRedirectWhenTargetDoesNotExist() {
        when(redirectCache.get(
                eq("Zz99Yy88"),
                any()
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Function<String, RedirectTarget> loader =
                    invocation.getArgument(1);

            return loader.apply("Zz99Yy88");
        });

        when(repository.findByShortCode("Zz99Yy88"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("Zz99Yy88"))
                .isInstanceOf(ShortUrlNotFoundException.class);

        verify(repository, never())
                .recordVisit(eq("Zz99Yy88"), any());
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

        RedirectTarget target = RedirectTarget.from(shortUrl);

        when(redirectCache.get(
                eq("Ab12Cd34"),
                any()
        )).thenReturn(target);

        when(repository.recordVisit("Ab12Cd34", NOW))
                .thenReturn(1);

        assertThat(service.resolve("Ab12Cd34"))
                .hasToString("https://example.com/resource");

        assertThat(
                meterRegistry.get("short_url.redirect")
                        .tag("outcome", "success")
                        .counter()
                        .count()
        ).isEqualTo(1.0);
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

        when(redirectCache.get(
                eq("Ab12Cd34"),
                any()
        )).thenReturn(RedirectTarget.from(expired));

        assertThatThrownBy(() -> service.resolve("Ab12Cd34"))
                .isInstanceOf(ShortUrlNotFoundException.class);

        verify(redirectCache).evict("Ab12Cd34");
    }
}