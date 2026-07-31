package com.yeshwanthk.agentic_url_shortener.url.repository;

import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}
