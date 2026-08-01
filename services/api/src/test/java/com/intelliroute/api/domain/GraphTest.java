package com.intelliroute.api.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraphTest {

    @Test
    @DisplayName("bidirectional edges create one directed edge in each direction")
    void bidirectionalEdges() {
        Graph graph = Graph.builder()
                .addNode(new Node("A", "Ashford", 0, 0))
                .addNode(new Node("B", "Brookhaven", 0, 0))
                .addBidirectionalEdge("A", "B", 5)
                .build();

        assertThat(graph.edgesFrom("A")).containsExactly(Edge.local("A", "B", 5));
        assertThat(graph.edgesFrom("B")).containsExactly(Edge.local("B", "A", 5));
        assertThat(graph.edgeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("bidirectional roads mirror full metadata in both directions")
    void bidirectionalRoadsMirrorMetadata() {
        Edge road = Edge.of("A", "B", 10, RoadType.HIGHWAY, 2.5, 9, 3, true);
        Graph graph = Graph.builder()
                .addNode(new Node("A", "Ashford", 0, 0))
                .addNode(new Node("B", "Brookhaven", 0, 0))
                .addBidirectionalRoad(road)
                .build();

        Edge reverse = graph.edgeBetween("B", "A");
        assertThat(reverse.distanceKm()).isEqualTo(10);
        assertThat(reverse.roadType()).isEqualTo(RoadType.HIGHWAY);
        assertThat(reverse.tollCost()).isEqualTo(2.5);
        assertThat(reverse.travelTimeMinutes()).isEqualTo(road.travelTimeMinutes());
    }

    @Test
    @DisplayName("travel time derives from road-type typical speed")
    void travelTimeDerivation() {
        // 10 km on a highway at 100 km/h -> 6.0 minutes
        assertThat(Edge.of("A", "B", 10, RoadType.HIGHWAY, 0, 9, 2, true)
                .travelTimeMinutes()).isEqualTo(6.0);
        // 10 km on a local road at 40 km/h -> 15.0 minutes
        assertThat(Edge.of("A", "B", 10, RoadType.LOCAL, 0, 6, 5, true)
                .travelTimeMinutes()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("duplicate node ids are rejected")
    void duplicateNodeRejected() {
        Graph.Builder builder = Graph.builder().addNode(new Node("A", "Ashford", 0, 0));

        assertThatThrownBy(() -> builder.addNode(new Node("A", "Other", 0, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    @DisplayName("edges referencing unknown nodes are rejected")
    void edgeWithUnknownNodeRejected() {
        Graph.Builder builder = Graph.builder().addNode(new Node("A", "Ashford", 0, 0));

        assertThatThrownBy(() -> builder.addEdge("A", "Z", 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown node");
    }

    @Test
    @DisplayName("invalid edge values are rejected")
    void invalidEdgeValuesRejected() {
        assertThatThrownBy(() -> Edge.local("A", "B", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Edge.local("A", "B", -2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Edge.local("A", "B", Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Edge.of("A", "B", 5, RoadType.LOCAL, -1, 5, 5, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tollCost");
        assertThatThrownBy(() -> Edge.of("A", "B", 5, RoadType.LOCAL, 0, 11, 5, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("safetyScore");
        assertThatThrownBy(() -> Edge.of("A", "B", 5, RoadType.LOCAL, 0, 5, -0.1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenicScore");
    }

    @Test
    @DisplayName("self-loop edges are rejected")
    void selfLoopRejected() {
        assertThatThrownBy(() -> Edge.local("A", "A", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Self-loop");
    }

    @Test
    @DisplayName("node and edge lookups for missing entries fail loudly")
    void missingLookups() {
        Graph graph = Graph.builder()
                .addNode(new Node("A", "Ashford", 0, 0))
                .addNode(new Node("B", "Brookhaven", 0, 0))
                .build();

        assertThatThrownBy(() -> graph.node("Z"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> graph.edgeBetween("A", "B"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No edge");
    }
}
