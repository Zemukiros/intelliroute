package com.intelliroute.api.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.intelliroute.api.domain.Edge;
import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.domain.Node;
import com.intelliroute.api.domain.RoadType;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Step 2 additions to the Dijkstra engine: closed-road handling and
 * node/edge exclusions (the primitive Yen's algorithm builds on).
 */
class DijkstraExclusionsTest {

    private DijkstraPathFinder pathFinder;
    private Graph graph;

    @BeforeEach
    void setUp() {
        pathFinder = new DijkstraPathFinder();
        // A - B - D plus a shortcut A - D that we can exclude, and a
        // closed road A - X - D that would be shortest if it were open.
        graph = Graph.builder()
                .addNode(new Node("A", "A-Town", 0, 0))
                .addNode(new Node("B", "B-Town", 0, 0))
                .addNode(new Node("D", "D-Town", 0, 0))
                .addNode(new Node("X", "X-Town", 0, 0))
                .addBidirectionalEdge("A", "B", 4)
                .addBidirectionalEdge("B", "D", 4)
                .addBidirectionalEdge("A", "D", 10)
                .addBidirectionalRoad(Edge.of("A", "X", 1, RoadType.LOCAL, 0, 5, 5, false))
                .addBidirectionalRoad(Edge.of("X", "D", 1, RoadType.LOCAL, 0, 5, 5, true))
                .build();
    }

    @Test
    @DisplayName("closed roads are ignored even when they would be shortest")
    void closedRoadIgnored() {
        PathResult result = pathFinder.findShortestPath(graph, "A", "D");

        assertThat(result.path()).containsExactly("A", "B", "D");
        assertThat(result.totalDistanceKm()).isEqualTo(8.0);
    }

    @Test
    @DisplayName("excluding an edge reroutes around it")
    void excludedEdgeRespected() {
        PathResult result = pathFinder.findShortestPath(graph, "A", "D",
                Set.of(), Set.of(DijkstraPathFinder.edgeKey("B", "D")));

        assertThat(result.path()).containsExactly("A", "D");
        assertThat(result.totalDistanceKm()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("excluding a node removes all its paths")
    void excludedNodeRespected() {
        PathResult result = pathFinder.findShortestPath(graph, "A", "D",
                Set.of("B"), Set.of());

        assertThat(result.path()).containsExactly("A", "D");
    }

    @Test
    @DisplayName("exclusions can make a destination unreachable")
    void exclusionsCanExhaustPaths() {
        PathResult result = pathFinder.findShortestPath(graph, "A", "D",
                Set.of("B"), Set.of(DijkstraPathFinder.edgeKey("A", "D")));

        assertThat(result.isReachable()).isFalse();
    }
}
