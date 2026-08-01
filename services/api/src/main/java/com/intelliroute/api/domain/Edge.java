package com.intelliroute.api.domain;

/**
 * A directed, weighted road segment between two nodes.
 *
 * @param from              id of the origin node
 * @param to                id of the destination node
 * @param distanceKm        road distance in kilometres; positive
 *                          (Dijkstra requires non-negative weights)
 * @param travelTimeMinutes estimated travel time in minutes; positive
 * @param roadType          classification of the segment
 * @param tollCost          toll for using this segment, in currency units; ≥ 0
 * @param safetyScore       0 (dangerous) .. 10 (very safe)
 * @param scenicScore       0 (dull) .. 10 (very scenic)
 * @param open              whether the road is currently usable; closed roads
 *                          are never used for routing but are still returned
 *                          by the network endpoint for visualization
 */
public record Edge(
        String from,
        String to,
        double distanceKm,
        double travelTimeMinutes,
        RoadType roadType,
        double tollCost,
        double safetyScore,
        double scenicScore,
        boolean open) {

    public Edge {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("Edge endpoints must not be blank");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("Self-loop edges are not allowed: " + from);
        }
        if (distanceKm <= 0 || !Double.isFinite(distanceKm)) {
            throw new IllegalArgumentException(
                    "Edge distance must be a positive finite number, got: " + distanceKm);
        }
        if (travelTimeMinutes <= 0 || !Double.isFinite(travelTimeMinutes)) {
            throw new IllegalArgumentException(
                    "Edge travel time must be a positive finite number, got: " + travelTimeMinutes);
        }
        if (roadType == null) {
            throw new IllegalArgumentException("Edge roadType must not be null");
        }
        if (tollCost < 0 || !Double.isFinite(tollCost)) {
            throw new IllegalArgumentException("Edge tollCost must be ≥ 0, got: " + tollCost);
        }
        requireScore(safetyScore, "safetyScore");
        requireScore(scenicScore, "scenicScore");
    }

    private static void requireScore(double value, String name) {
        if (value < 0 || value > 10 || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be within 0..10, got: " + value);
        }
    }

    public boolean isHighway() {
        return roadType.isHighway();
    }

    /**
     * Convenience factory for a plain open local road with derived travel
     * time and neutral metadata. Used by tests and simple graphs.
     */
    public static Edge local(String from, String to, double distanceKm) {
        return of(from, to, distanceKm, RoadType.LOCAL, 0, 6, 5, true);
    }

    /**
     * Factory deriving travel time deterministically from distance and the
     * road type's typical speed: {@code minutes = distance / speed * 60}.
     */
    public static Edge of(String from, String to, double distanceKm, RoadType type,
                          double tollCost, double safetyScore, double scenicScore, boolean open) {
        double minutes = distanceKm / type.typicalSpeedKmh() * 60.0;
        return new Edge(from, to, distanceKm, round1(minutes), type,
                tollCost, safetyScore, scenicScore, open);
    }

    /** Returns a copy of this edge with from/to swapped (for two-way roads). */
    public Edge reversed() {
        return new Edge(to, from, distanceKm, travelTimeMinutes, roadType,
                tollCost, safetyScore, scenicScore, open);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
