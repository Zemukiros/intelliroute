package com.intelliroute.api.config;

import static com.intelliroute.api.domain.RoadType.ARTERIAL;
import static com.intelliroute.api.domain.RoadType.HIGHWAY;
import static com.intelliroute.api.domain.RoadType.LOCAL;
import static com.intelliroute.api.domain.RoadType.SCENIC;

import com.intelliroute.api.domain.Edge;
import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.domain.Node;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the local sample road network used during development.
 *
 * <p>A fictional region of ten towns connected by two-way roads. All values
 * are deterministic and documented here — nothing is randomized at startup.
 * Distances are in kilometres; travel times derive from each road type's
 * typical speed (see {@link com.intelliroute.api.domain.RoadType}); safety
 * and scenic scores are 0..10.
 *
 * <p>Deliberate demonstration features:
 * <ul>
 *   <li>Node "J" (Juniper Isle) is disconnected — exercises unreachable
 *       destinations end to end.</li>
 *   <li>Road B–E is closed (under construction) — exercises closed-road
 *       exclusion and visualization.</li>
 *   <li>The A→F pair has three distinct candidate routes with real
 *       trade-offs:
 *       <ul>
 *         <li>A-C-D-F — 14 km, shortest, mixed local/highway, 3.00 toll</li>
 *         <li>A-B-D-F — 17 km, fastest (~12 min) and safest (highways),
 *             but 5.50 in tolls and 71% highway</li>
 *         <li>A-C-E-F — 18 km, toll-free, highway-free, most scenic (8.3)</li>
 *       </ul>
 *       so preference ranking produces visibly different winners.</li>
 * </ul>
 *
 * <p>In a later milestone this bean is replaced by a persistent
 * network store without touching the engine or web layers.
 */
@Configuration
public class SampleNetworkConfig {

    @Bean
    public Graph roadNetwork() {
        return Graph.builder()
                .addNode(new Node("A", "Ashford", 80, 210))
                .addNode(new Node("B", "Brookhaven", 240, 80))
                .addNode(new Node("C", "Cedarville", 260, 300))
                .addNode(new Node("D", "Dunmore", 450, 190))
                .addNode(new Node("E", "Eastvale", 470, 380))
                .addNode(new Node("F", "Fairmont", 650, 280))
                .addNode(new Node("G", "Glenrock", 460, 40))
                .addNode(new Node("H", "Harborview", 680, 90))
                .addNode(new Node("I", "Ironbridge", 660, 470))
                .addNode(new Node("J", "Juniper Isle", 90, 450))
                // Two-way roads:            from, to,  km, type,     toll, safety, scenic, open
                .addBidirectionalRoad(Edge.of("A", "B", 5, ARTERIAL, 0.00, 7, 4, true))
                .addBidirectionalRoad(Edge.of("A", "C", 4, LOCAL,    0.00, 6, 6, true))
                .addBidirectionalRoad(Edge.of("B", "C", 6, LOCAL,    0.00, 6, 5, true))
                .addBidirectionalRoad(Edge.of("B", "D", 7, HIGHWAY,  2.50, 9, 2, true))
                .addBidirectionalRoad(Edge.of("B", "G", 6, HIGHWAY,  0.00, 9, 3, true))
                .addBidirectionalRoad(Edge.of("C", "D", 5, LOCAL,    0.00, 5, 5, true))
                .addBidirectionalRoad(Edge.of("C", "E", 8, SCENIC,   0.00, 7, 9, true))
                .addBidirectionalRoad(Edge.of("D", "E", 4, LOCAL,    0.00, 6, 6, true))
                .addBidirectionalRoad(Edge.of("D", "F", 5, HIGHWAY,  3.00, 9, 2, true))
                .addBidirectionalRoad(Edge.of("D", "G", 5, ARTERIAL, 0.00, 7, 4, true))
                .addBidirectionalRoad(Edge.of("E", "F", 6, SCENIC,   0.00, 7, 9, true))
                .addBidirectionalRoad(Edge.of("E", "I", 7, SCENIC,   0.00, 6, 8, true))
                .addBidirectionalRoad(Edge.of("F", "H", 4, ARTERIAL, 0.00, 8, 5, true))
                .addBidirectionalRoad(Edge.of("F", "I", 5, LOCAL,    0.00, 6, 6, true))
                .addBidirectionalRoad(Edge.of("G", "H", 6, HIGHWAY,  0.00, 9, 3, true))
                // Closed for construction — visible on the map, never routed:
                .addBidirectionalRoad(Edge.of("B", "E", 9, LOCAL,    0.00, 5, 5, false))
                // "J" (Juniper Isle) has no road connections on purpose.
                .build();
    }
}
