package com.intelliroute.api.web.dto;

import java.util.List;

/**
 * Road network description consumed by the frontend visualization.
 * Closed roads are included (rendered differently) but never routed.
 */
public record NetworkResponse(List<NodeDto> nodes, List<EdgeDto> edges) {

    public record NodeDto(String id, String name, double x, double y) {}

    public record EdgeDto(
            String from,
            String to,
            double distanceKm,
            double travelTimeMinutes,
            String roadType,
            double tollCost,
            double safetyScore,
            double scenicScore,
            boolean open) {}
}
