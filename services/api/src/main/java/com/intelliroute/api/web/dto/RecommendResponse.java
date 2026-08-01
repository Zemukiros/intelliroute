package com.intelliroute.api.web.dto;

import com.intelliroute.api.engine.RouteCandidate;
import com.intelliroute.api.ranking.RankingDtos.ParsedPreferences;
import com.intelliroute.api.ranking.RankingDtos.RankedRoute;
import java.util.List;

/**
 * Full recommendation result: candidates, how the preference was parsed,
 * the ranking with explanations, and measured timings.
 *
 * @param provider        ranking provider that produced the ordering
 *                        (e.g. "local-deterministic-v1" or
 *                        "java-local-fallback")
 * @param fallbackUsed    true when the ranking service was unavailable and
 *                        the local distance-ordered fallback was applied
 * @param routingTimeMs   measured time to generate candidate routes
 * @param rankingTimeMs   measured time of the ranking step (incl. HTTP)
 */
public record RecommendResponse(
        String origin,
        String destination,
        String preference,
        String provider,
        boolean fallbackUsed,
        ParsedPreferences parsedPreferences,
        List<RouteCandidate> candidates,
        List<RankedRoute> rankedRoutes,
        String recommendedRouteId,
        double routingTimeMs,
        double rankingTimeMs) {
}
