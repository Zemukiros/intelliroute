package com.intelliroute.api.engine;

import java.util.List;

/**
 * Outcome of a shortest-path search.
 *
 * @param path            ordered node ids from origin to destination;
 *                        empty when the destination is unreachable
 * @param totalDistanceKm sum of edge weights along the path;
 *                        {@link Double#POSITIVE_INFINITY} when unreachable
 * @param visitedNodes    number of nodes settled by the algorithm before
 *                        terminating — a measure of search effort
 */
public record PathResult(List<String> path, double totalDistanceKm, int visitedNodes) {

    public boolean isReachable() {
        return !path.isEmpty();
    }

    public static PathResult unreachable(int visitedNodes) {
        return new PathResult(List.of(), Double.POSITIVE_INFINITY, visitedNodes);
    }
}
