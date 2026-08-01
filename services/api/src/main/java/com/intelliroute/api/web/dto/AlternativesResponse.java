package com.intelliroute.api.web.dto;

import com.intelliroute.api.engine.RouteCandidate;
import java.util.List;

/**
 * Result of a k-shortest-paths search with aggregated route metadata.
 *
 * @param executionTimeMs     measured wall-clock time of the search
 * @param dijkstraInvocations number of Dijkstra searches Yen's algorithm ran
 * @param totalVisitedNodes   aggregate settled-node count across searches
 */
public record AlternativesResponse(
        String origin,
        String destination,
        int requestedRoutes,
        int returnedRoutes,
        List<RouteCandidate> routes,
        double executionTimeMs,
        int dijkstraInvocations,
        int totalVisitedNodes) {
}
