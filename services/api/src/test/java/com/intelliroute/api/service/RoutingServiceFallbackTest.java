package com.intelliroute.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.intelliroute.api.config.SampleNetworkConfig;
import com.intelliroute.api.engine.DijkstraPathFinder;
import com.intelliroute.api.engine.YenKShortestPaths;
import com.intelliroute.api.ranking.RankingClient;
import com.intelliroute.api.ranking.RankingProperties;
import com.intelliroute.api.web.dto.RecommendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Verifies graceful degradation when the ranking service is unreachable:
 * the client points at a closed local port, so every call fails fast and
 * the local distance-ordered fallback must kick in.
 */
class RoutingServiceFallbackTest {

    private RoutingService service;

    @BeforeEach
    void setUp() {
        RankingClient unreachableClient = new RankingClient(
                new RankingProperties("http://127.0.0.1:59999", 200, 200),
                RestClient.builder());
        DijkstraPathFinder dijkstra = new DijkstraPathFinder();
        service = new RoutingService(
                new SampleNetworkConfig().roadNetwork(),
                dijkstra,
                new YenKShortestPaths(dijkstra),
                unreachableClient);
    }

    @Test
    @DisplayName("recommendation falls back to distance ordering and says so")
    void fallbackRanking() {
        RecommendResponse response =
                service.recommend("A", "F", "avoid tolls", 3);

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.provider()).isEqualTo(RoutingService.FALLBACK_PROVIDER);
        assertThat(response.recommendedRouteId()).isEqualTo("route-1");
        assertThat(response.rankedRoutes()).hasSize(3);
        assertThat(response.rankedRoutes().get(0).explanation())
                .contains("Preference ranking was unavailable");
        assertThat(response.parsedPreferences().confidence()).isZero();
        assertThat(response.routingTimeMs()).isGreaterThanOrEqualTo(0.0);
        assertThat(response.rankingTimeMs()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("fallback ranking is deterministic: candidates stay distance-ordered")
    void fallbackDeterministic() {
        RecommendResponse first = service.recommend("A", "F", "scenic", 3);
        RecommendResponse second = service.recommend("A", "F", "scenic", 3);

        assertThat(first.rankedRoutes().stream().map(r -> r.routeId()).toList())
                .isEqualTo(second.rankedRoutes().stream().map(r -> r.routeId()).toList());
        assertThat(first.candidates().get(0).totalDistanceKm())
                .isLessThanOrEqualTo(first.candidates().get(1).totalDistanceKm());
    }

    @Test
    @DisplayName("health probe reports unreachable ranking service as unhealthy")
    void healthProbe() {
        RankingClient unreachableClient = new RankingClient(
                new RankingProperties("http://127.0.0.1:59999", 200, 200),
                RestClient.builder());

        assertThat(unreachableClient.isHealthy()).isFalse();
    }
}
