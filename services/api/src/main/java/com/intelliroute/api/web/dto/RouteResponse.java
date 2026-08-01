package com.intelliroute.api.web.dto;

import java.util.List;

/**
 * Successful route calculation result.
 *
 * @param executionTimeMs measured wall-clock time of the path-finding
 *                        algorithm itself (excludes HTTP overhead)
 */
public record RouteResponse(
        String origin,
        String destination,
        List<String> path,
        double totalDistance,
        int visitedNodes,
        double executionTimeMs) {
}
