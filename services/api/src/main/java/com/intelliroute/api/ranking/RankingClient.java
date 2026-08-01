package com.intelliroute.api.ranking;

import com.intelliroute.api.engine.RouteCandidate;
import com.intelliroute.api.ranking.RankingDtos.RankRequest;
import com.intelliroute.api.ranking.RankingDtos.RankResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the preference-ranking service.
 *
 * <p>Failure philosophy: every transport or contract problem is translated
 * into {@link RankingServiceException} so the routing service can apply its
 * local fallback — a ranking outage must never break route recommendation.
 * Logs never include request bodies (user preference text stays out of
 * info-level logs).
 */
@Component
public class RankingClient {

    private static final Logger log = LoggerFactory.getLogger(RankingClient.class);

    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public RankingClient(RankingProperties properties, RestClient.Builder builder) {
        this(buildDefaultClient(properties, builder));
    }

    /** Test seam: inject a fully built client (e.g. bound to a mock server). */
    RankingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient buildDefaultClient(RankingProperties properties,
                                                 RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Ranks candidates against a natural-language preference.
     *
     * @throws RankingServiceException when the service is unreachable, times
     *                                 out, or returns an unusable response
     */
    public RankResponse rank(String preference, List<RouteCandidate> candidates) {
        try {
            RankResponse response = restClient.post()
                    .uri("/rank")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RankRequest(preference, candidates))
                    .retrieve()
                    .body(RankResponse.class);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw new RankingServiceException("Ranking service returned an empty result");
            }
            return response;
        } catch (RankingServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Ranking service call failed ({}): {}",
                    e.getClass().getSimpleName(), e.getMessage());
            throw new RankingServiceException("Ranking service unavailable", e);
        }
    }

    /** Lightweight availability probe against {@code GET /health}. */
    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
