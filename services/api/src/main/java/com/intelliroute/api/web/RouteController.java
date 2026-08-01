package com.intelliroute.api.web;

import com.intelliroute.api.service.RoutingService;
import com.intelliroute.api.web.dto.AlternativesRequest;
import com.intelliroute.api.web.dto.AlternativesResponse;
import com.intelliroute.api.web.dto.NetworkResponse;
import com.intelliroute.api.web.dto.RecommendRequest;
import com.intelliroute.api.web.dto.RecommendResponse;
import com.intelliroute.api.web.dto.RouteRequest;
import com.intelliroute.api.web.dto.RouteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for route calculation, alternatives, recommendation, and
 * network inspection.
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RoutingService routingService;

    public RouteController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Calculates the single shortest route between two nodes.
     *
     * <p>{@code POST /api/routes/calculate}
     */
    @PostMapping("/calculate")
    public RouteResponse calculate(@Valid @RequestBody RouteRequest request) {
        return routingService.calculateRoute(
                request.origin().trim(), request.destination().trim());
    }

    /**
     * Generates up to N alternative loopless routes with road metadata.
     *
     * <p>{@code POST /api/routes/alternatives}
     */
    @PostMapping("/alternatives")
    public AlternativesResponse alternatives(@Valid @RequestBody AlternativesRequest request) {
        return routingService.findAlternatives(
                request.origin().trim(), request.destination().trim(),
                request.maximumRoutesOrDefault());
    }

    /**
     * Generates alternatives and ranks them against a natural-language
     * preference via the ranking service (with local fallback).
     *
     * <p>{@code POST /api/routes/recommend}
     */
    @PostMapping("/recommend")
    public RecommendResponse recommend(@Valid @RequestBody RecommendRequest request) {
        return routingService.recommend(
                request.origin().trim(), request.destination().trim(),
                request.preference().trim(), request.maximumRoutesOrDefault());
    }

    /**
     * Returns the full road network for visualization, including road
     * metadata and closed segments.
     *
     * <p>{@code GET /api/routes/network}
     */
    @GetMapping("/network")
    public NetworkResponse network() {
        var graph = routingService.network();
        return new NetworkResponse(
                graph.nodes().stream()
                        .map(n -> new NetworkResponse.NodeDto(n.id(), n.name(), n.x(), n.y()))
                        .toList(),
                graph.edges().stream()
                        // Emit each two-way road once for rendering (from < to).
                        .filter(e -> e.from().compareTo(e.to()) < 0)
                        .map(e -> new NetworkResponse.EdgeDto(
                                e.from(), e.to(), e.distanceKm(), e.travelTimeMinutes(),
                                e.roadType().name(), e.tollCost(), e.safetyScore(),
                                e.scenicScore(), e.open()))
                        .toList());
    }
}
