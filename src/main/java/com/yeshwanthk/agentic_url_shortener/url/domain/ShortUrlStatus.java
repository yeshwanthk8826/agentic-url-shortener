package com.yeshwanthk.agentic_url_shortener.url.domain;

/**
 * Represents the lifecycle state of a shortened URL.
 *
 * The enum values intentionally match the database check constraint defined
 * in V1__create_short_urls_table.sql.
 */
public enum ShortUrlStatus {

    /**
     * The shortened URL can be resolved and redirected.
     */
    ACTIVE,

    /**
     * The shortened URL was administratively disabled.
     */
    DISABLED,

    /**
     * The shortened URL has passed its configured expiration time.
     */
    EXPIRED
}