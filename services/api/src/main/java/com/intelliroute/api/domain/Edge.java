package com.intelliroute.api.domain;

/**
 * A directed, weighted road segment between two nodes.
 *
 * @param from       id of the origin node
 * @param to         id of the destination node
 * @param distanceKm road distance in kilometres; must be positive
 *                   (Dijkstra's algorithm requires non-negative weights)
 */
public record Edge(String from, String to, double distanceKm) {

    public Edge {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("Edge endpoints must not be blank");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("Self-loop edges are not allowed: " + from);
        }
        if (distanceKm <= 0 || !Double.isFinite(distanceKm)) {
            throw new IllegalArgumentException(
                    "Edge weight must be a positive finite number, got: " + distanceKm);
        }
    }
}
