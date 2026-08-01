package com.intelliroute.api.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the external preference-ranking service.
 *
 * @param baseUrl          root URL of the FastAPI ranking service
 * @param connectTimeoutMs TCP connect timeout
 * @param readTimeoutMs    response read timeout
 */
@ConfigurationProperties(prefix = "intelliroute.ranking")
public record RankingProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs) {

    public RankingProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8090";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 1000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 2000;
        }
    }
}
