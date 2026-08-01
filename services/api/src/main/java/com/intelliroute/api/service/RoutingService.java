package com.intelliroute.api.service;

import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.engine.DijkstraPathFinder;
import com.intelliroute.api.engine.PathResult;
import com.intelliroute.api.engine.RouteCandidate;
import com.intelliroute.api.engine.YenKShortestPaths;
import com.intelliroute.api.ranking.RankingClient;
import com.intelliroute.api.ranking.RankingDtos.ParsedPreferences;
import com.intelliroute.api.ranking.RankingDtos.RankResponse;
import com.intelliroute.api.ranking.RankingDtos.RankedRoute;
import com.intelliroute.api.ranking.RankingServiceException;
import com.intelliroute.api.web.dto.AlternativesResponse;
import com.intelliroute.api.web.dto.RecommendResponse;
import com.intelliroute.api.web.dto.RouteResponse;
import com.intelliroute.api.web.error.RouteUnreachableException;
import com.intelliroute.api.web.error.UnknownNodeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service coordinating route calculation, alternative-route
 * generation, and preference-based recommendation.
 *
 * <p>All reported timings are measured with {@link System#nanoTime()} around
 * the actual work — never estimated or hard-coded. When the external ranking
 * service is unavailable, recommendation degrades gracefully to a local
 * distance-ordered fallback and says so in the response.
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    public static final String FALLBACK_PROVIDER = "java-local-fallback";

    private final Graph roadNetwork;
    private final DijkstraPathFinder pathFinder;
    private final YenKShortestPaths kShortestPaths;
    private final RankingClient rankingClient;

    public RoutingService(Graph roadNetwork, DijkstraPathFinder pathFinder,
                          YenKShortestPaths kShortestPaths, RankingClient rankingClient) {
        this.roadNetwork = roadNetwork;
        this.pathFinder = pathFinder;
        this.kShortestPaths = kShortestPaths;
        this.rankingClient = rankingClient;
    }

    /** Shortest route between two nodes (Step 1 behaviour, unchanged). */
    public RouteResponse calculateRoute(String originId, String destinationId) {
        requireKnownNode(originId, "origin");
        requireKnownNode(destinationId, "destination");

        long startNanos = System.nanoTime();
        PathResult result = pathFinder.findShortestPath(roadNetwork, originId, destinationId);
        double executionTimeMs = elapsedMs(startNanos);

        if (!result.isReachable()) {
            throw new RouteUnreachableException(originId, destinationId, result.visitedNodes());
        }

        return new RouteResponse(
                originId,
                destinationId,
                result.path(),
                result.totalDistanceKm(),
                result.visitedNodes(),
                executionTimeMs);
    }

    /** Up to {@code maxRoutes} loopless candidate routes, shortest first. */
    public AlternativesResponse findAlternatives(String originId, String destinationId,
                                                 int maxRoutes) {
        requireKnownNode(originId, "origin");
        requireKnownNode(destinationId, "destination");

        long startNanos = System.nanoTime();
        YenKShortestPaths.SearchResult search =
                kShortestPaths.findKShortestPaths(roadNetwork, originId, destinationId, maxRoutes);
        double executionTimeMs = elapsedMs(startNanos);

        if (search.paths().isEmpty()) {
            throw new RouteUnreachableException(originId, destinationId,
                    search.totalVisitedNodes());
        }

        List<RouteCandidate> candidates = toCandidates(search);
        return new AlternativesResponse(
                originId, destinationId, maxRoutes, candidates.size(), candidates,
                executionTimeMs, search.dijkstraInvocations(), search.totalVisitedNodes());
    }

    /**
     * Full recommendation flow: generate candidates, then rank them against
     * the natural-language preference via the ranking service (or the local
     * fallback when it is unavailable).
     */
    public RecommendResponse recommend(String originId, String destinationId,
                                       String preference, int maxRoutes) {
        long routingStart = System.nanoTime();
        AlternativesResponse alternatives = findAlternatives(originId, destinationId, maxRoutes);
        double routingTimeMs = elapsedMs(routingStart);

        List<RouteCandidate> candidates = alternatives.routes();

        long rankingStart = System.nanoTime();
        RankResponse ranking;
        boolean fallbackUsed = false;
        try {
            ranking = rankingClient.rank(preference, candidates);
        } catch (RankingServiceException e) {
            log.info("Ranking service unavailable — using local fallback ranking");
            ranking = localFallbackRanking(candidates);
            fallbackUsed = true;
        }
        double rankingTimeMs = elapsedMs(rankingStart);

        return new RecommendResponse(
                originId,
                destinationId,
                preference,
                ranking.provider(),
                fallbackUsed,
                ranking.parsedPreferences(),
                candidates,
                ranking.results(),
                ranking.recommendedRouteId(),
                routingTimeMs,
                rankingTimeMs);
    }

    /**
     * Deterministic local fallback: candidates are already ordered by total
     * distance (shortest first), so rank them in that order and say clearly
     * that no preference interpretation happened.
     */
    private RankResponse localFallbackRanking(List<RouteCandidate> candidates) {
        List<RankedRoute> results = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            RouteCandidate c = candidates.get(i);
            results.add(new RankedRoute(
                    c.routeId(),
                    i + 1,
                    round4(1.0 - (double) i / Math.max(1, candidates.size())),
                    i == 0
                            ? "Shortest route by distance (" + c.totalDistanceKm()
                                    + " km). Preference ranking was unavailable, so routes"
                                    + " are ordered by distance."
                            : "Ordered by total distance (" + c.totalDistanceKm() + " km)."));
        }
        ParsedPreferences parsed = new ParsedPreferences(
                Map.of(), List.of(), List.of(), 0.0,
                "Ranking service unreachable — preference text was not interpreted.");
        return new RankResponse(FALLBACK_PROVIDER, parsed, results,
                candidates.get(0).routeId());
    }

    private List<RouteCandidate> toCandidates(YenKShortestPaths.SearchResult search) {
        List<RouteCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < search.paths().size(); i++) {
            candidates.add(RouteCandidate.fromPath(
                    "route-" + (i + 1), roadNetwork, search.paths().get(i).path()));
        }
        return List.copyOf(candidates);
    }

    public Graph network() {
        return roadNetwork;
    }

    private void requireKnownNode(String nodeId, String role) {
        if (!roadNetwork.containsNode(nodeId)) {
            throw new UnknownNodeException(nodeId, role);
        }
    }

    private static double elapsedMs(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / NANOS_PER_MILLI * 1000.0) / 1000.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
