package com.intelliroute.api.ranking;

import com.intelliroute.api.engine.RouteCandidate;
import java.util.List;
import java.util.Map;

/**
 * Wire contract shared with the Python ranking service
 * (see services/ai-service and docs/ROUTE_RANKING.md).
 */
public final class RankingDtos {

    private RankingDtos() {}

    /** Request sent to {@code POST {ranking}/rank}. */
    public record RankRequest(String preference, List<RouteCandidate> candidates) {}

    /** How the service interpreted the natural-language preference. */
    public record ParsedPreferences(
            Map<String, Double> weights,
            List<String> recognizedTerms,
            List<String> unrecognizedTerms,
            double confidence,
            String note) {}

    /** One ranked route. */
    public record RankedRoute(String routeId, int rank, double score, String explanation) {}

    /** Full ranking outcome. */
    public record RankResponse(
            String provider,
            ParsedPreferences parsedPreferences,
            List<RankedRoute> results,
            String recommendedRouteId) {}
}
