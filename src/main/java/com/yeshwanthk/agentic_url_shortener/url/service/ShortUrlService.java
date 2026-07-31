package com.yeshwanthk.agentic_url_shortener.url.service;

import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectCache;
import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectTarget;
import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import com.yeshwanthk.agentic_url_shortener.url.dto.ShortUrlResponse;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortCodeGenerator;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import com.yeshwanthk.agentic_url_shortener.url.dto.UrlAnalyticsResponse;
import com.yeshwanthk.agentic_url_shortener.url.exception.InvalidUrlException;
import com.yeshwanthk.agentic_url_shortener.url.exception.ShortCodeGenerationException;
import com.yeshwanthk.agentic_url_shortener.url.exception.ShortUrlNotFoundException;
import com.yeshwanthk.agentic_url_shortener.url.repository.ShortUrlRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class ShortUrlService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedirectCache redirectCache;
    private final Clock clock;
    private final String publicBaseUrl;

    private final Counter createdCounter;
    private final Counter redirectSuccessCounter;
    private final Counter redirectFailureCounter;
    private final Timer redirectTimer;

    public ShortUrlService(
            ShortUrlRepository repository,
            ShortCodeGenerator shortCodeGenerator,
            RedirectCache redirectCache,
            Clock clock,
            MeterRegistry meterRegistry,
            @Value("${app.public-base-url:http://localhost:8080}")
            String publicBaseUrl
    ) {
        this.repository = repository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.redirectCache = redirectCache;
        this.clock = clock;
        this.publicBaseUrl = removeTrailingSlash(publicBaseUrl);

        this.createdCounter = Counter.builder("short_url.created")
                .description("Number of shortened URLs created")
                .register(meterRegistry);

        this.redirectSuccessCounter =
                Counter.builder("short_url.redirect")
                        .description("Number of successful redirects")
                        .tag("outcome", "success")
                        .register(meterRegistry);

        this.redirectFailureCounter =
                Counter.builder("short_url.redirect")
                        .description("Number of failed redirects")
                        .tag("outcome", "failure")
                        .register(meterRegistry);

        this.redirectTimer = Timer.builder("short_url.redirect.duration")
                .description("Time spent resolving redirects")
                .register(meterRegistry);
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        URI validatedUri = validateAndParse(request.url());
        String normalizedUrl = normalize(validatedUri);
        String shortCode = allocateShortCode();

        ShortUrl entity = ShortUrl.create(
                shortCode,
                request.url(),
                normalizedUrl,
                clock.instant(),
                request.expiresAt()
        );

        ShortUrl saved = repository.save(entity);

        redirectCache.put(
                saved.getShortCode(),
                RedirectTarget.from(saved)
        );

        createdCounter.increment();

        return ShortUrlResponse.from(saved, publicBaseUrl);
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse findByShortCode(String shortCode) {
        ShortUrl shortUrl = getResolvableShortUrl(shortCode);
        return ShortUrlResponse.from(shortUrl, publicBaseUrl);
    }

    @Transactional
    public URI resolve(String shortCode) {
        long startedAt = System.nanoTime();

        try {
            Instant now = clock.instant();

            RedirectTarget target = redirectCache.get(
                    shortCode,
                    this::loadRedirectTarget
            );

            if (target == null || !target.isResolvableAt(now)) {
                redirectCache.evict(shortCode);
                redirectFailureCounter.increment();
                throw new ShortUrlNotFoundException(shortCode);
            }

            int updatedRows = repository.recordVisit(shortCode, now);

            if (updatedRows != 1) {
                redirectCache.evict(shortCode);
                redirectFailureCounter.increment();
                throw new ShortUrlNotFoundException(shortCode);
            }

            redirectSuccessCounter.increment();
            return URI.create(target.originalUrl());
        } finally {
            redirectTimer.record(
                    System.nanoTime() - startedAt,
                    TimeUnit.NANOSECONDS
            );
        }
    }

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse analytics(String shortCode) {
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        return UrlAnalyticsResponse.from(shortUrl);
    }

    private RedirectTarget loadRedirectTarget(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(RedirectTarget::from)
                .orElse(null);
    }

    private ShortUrl getResolvableShortUrl(String shortCode) {
        Instant now = clock.instant();

        return repository.findByShortCode(shortCode)
                .filter(shortUrl -> shortUrl.isResolvableAt(now))
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
    }

    private String allocateShortCode() {
        for (int attempt = 0;
             attempt < MAX_CODE_GENERATION_ATTEMPTS;
             attempt++) {

            String candidate = shortCodeGenerator.generate();

            if (!repository.existsByShortCode(candidate)) {
                return candidate;
            }
        }

        throw new ShortCodeGenerationException();
    }

    private URI validateAndParse(String originalUrl) {
        try {
            URI uri = new URI(originalUrl.trim());
            String scheme = uri.getScheme();

            if (scheme == null) {
                throw new InvalidUrlException("URL scheme is required");
            }

            String normalizedScheme =
                    scheme.toLowerCase(Locale.ROOT);

            if (!normalizedScheme.equals("http")
                    && !normalizedScheme.equals("https")) {
                throw new InvalidUrlException(
                        "Only HTTP and HTTPS URLs are supported"
                );
            }

            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException(
                        "URL must contain a valid host"
                );
            }

            if (uri.getUserInfo() != null) {
                throw new InvalidUrlException(
                        "URLs containing embedded credentials are not supported"
                );
            }

            return uri;
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException(
                    "URL is not syntactically valid"
            );
        }
    }

    private String normalize(URI uri) {
        try {
            String scheme =
                    uri.getScheme().toLowerCase(Locale.ROOT);
            String host =
                    uri.getHost().toLowerCase(Locale.ROOT);
            int port = normalizePort(scheme, uri.getPort());

            URI normalized = new URI(
                    scheme,
                    null,
                    host,
                    port,
                    emptyPathAsSlash(uri.getPath()),
                    uri.getQuery(),
                    null
            ).normalize();

            return normalized.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException(
                    "URL could not be normalized"
            );
        }
    }

    private int normalizePort(String scheme, int port) {
        if (("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443)) {
            return -1;
        }

        return port;
    }

    private String emptyPathAsSlash(String path) {
        return path == null || path.isBlank() ? "/" : path;
    }

    private static String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}