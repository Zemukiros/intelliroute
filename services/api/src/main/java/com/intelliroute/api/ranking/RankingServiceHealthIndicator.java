package com.intelliroute.api.ranking;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Surfaces ranking-service availability under {@code /actuator/health}.
 * The routing API stays UP either way — ranking has a local fallback — so
 * this reports detail rather than gating overall health.
 */
@Component("rankingService")
public class RankingServiceHealthIndicator implements HealthIndicator {

    private final RankingClient rankingClient;

    public RankingServiceHealthIndicator(RankingClient rankingClient) {
        this.rankingClient = rankingClient;
    }

    @Override
    public Health health() {
        return rankingClient.isHealthy()
                ? Health.up().withDetail("mode", "remote-ranking").build()
                : Health.up().withDetail("mode", "local-fallback")
                        .withDetail("reason", "ranking service unreachable").build();
    }
}
