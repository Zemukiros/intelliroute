package com.intelliroute.api.engine;

import com.intelliroute.api.domain.Edge;
import com.intelliroute.api.domain.Graph;
import java.util.List;

/**
 * A candidate route with aggregated road metadata, produced by the
 * k-shortest-paths search.
 *
 * @param routeId            stable identifier within one response ("route-1"…)
 * @param path               ordered node ids from origin to destination
 * @param totalDistanceKm    sum of segment distances
 * @param travelTimeMinutes  sum of segment travel-time estimates
 * @param tollCost           sum of segment tolls
 * @param safetyScore        distance-weighted average safety (0..10)
 * @param scenicScore        distance-weighted average scenery (0..10)
 * @param highwayPercentage  share of distance on highway segments (0..100)
 */
public record RouteCandidate(
        String routeId,
        List<String> path,
        double totalDistanceKm,
        double travelTimeMinutes,
        double tollCost,
        double safetyScore,
        double scenicScore,
        double highwayPercentage) {

    /** Computes aggregate metrics for a node path over the given graph. */
    public static RouteCandidate fromPath(String routeId, Graph graph, List<String> path) {
        if (path.size() < 2) {
            return new RouteCandidate(routeId, List.copyOf(path), 0, 0, 0, 0, 0, 0);
        }
        double distance = 0;
        double time = 0;
        double toll = 0;
        double safetyWeighted = 0;
        double scenicWeighted = 0;
        double highwayDistance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Edge edge = graph.edgeBetween(path.get(i), path.get(i + 1));
            distance += edge.distanceKm();
            time += edge.travelTimeMinutes();
            toll += edge.tollCost();
            safetyWeighted += edge.safetyScore() * edge.distanceKm();
            scenicWeighted += edge.scenicScore() * edge.distanceKm();
            if (edge.isHighway()) {
                highwayDistance += edge.distanceKm();
            }
        }
        return new RouteCandidate(
                routeId,
                List.copyOf(path),
                round2(distance),
                round1(time),
                round2(toll),
                round2(safetyWeighted / distance),
                round2(scenicWeighted / distance),
                round1(highwayDistance / distance * 100.0));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
