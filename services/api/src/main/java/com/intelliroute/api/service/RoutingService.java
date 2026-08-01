package com.intelliroute.api.service;

import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.engine.DijkstraPathFinder;
import com.intelliroute.api.engine.PathResult;
import com.intelliroute.api.web.dto.RouteResponse;
import com.intelliroute.api.web.error.RouteUnreachableException;
import com.intelliroute.api.web.error.UnknownNodeException;
import org.springframework.stereotype.Service;

/**
 * Application service coordinating route calculations.
 *
 * <p>Validates node existence, delegates to the path-finding engine, and
 * measures actual algorithm execution time with {@link System#nanoTime()}.
 */
@Service
public class RoutingService {

    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private final Graph roadNetwork;
    private final DijkstraPathFinder pathFinder;

    public RoutingService(Graph roadNetwork, DijkstraPathFinder pathFinder) {
        this.roadNetwork = roadNetwork;
        this.pathFinder = pathFinder;
    }

    /**
     * Calculates the shortest route between two nodes of the road network.
     *
     * @throws UnknownNodeException      when either node id does not exist
     * @throws RouteUnreachableException when no path connects the two nodes
     */
    public RouteResponse calculateRoute(String originId, String destinationId) {
        requireKnownNode(originId, "origin");
        requireKnownNode(destinationId, "destination");

        long startNanos = System.nanoTime();
        PathResult result = pathFinder.findShortestPath(roadNetwork, originId, destinationId);
        long elapsedNanos = System.nanoTime() - startNanos;
        double executionTimeMs = Math.round(elapsedNanos / NANOS_PER_MILLI * 1000.0) / 1000.0;

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

    public Graph network() {
        return roadNetwork;
    }

    private void requireKnownNode(String nodeId, String role) {
        if (!roadNetwork.containsNode(nodeId)) {
            throw new UnknownNodeException(nodeId, role);
        }
    }
}
