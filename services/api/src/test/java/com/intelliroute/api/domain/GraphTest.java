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

        assertThat(graph.edgesFrom("A")).containsExactly(new Edge("A", "B", 5));
        assertThat(graph.edgesFrom("B")).containsExactly(new Edge("B", "A", 5));
        assertThat(graph.edgeCount()).isEqualTo(2);
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
    @DisplayName("non-positive and non-finite edge weights are rejected")
    void invalidWeightsRejected() {
        assertThatThrownBy(() -> new Edge("A", "B", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Edge("A", "B", -2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Edge("A", "B", Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("self-loop edges are rejected")
    void selfLoopRejected() {
        assertThatThrownBy(() -> new Edge("A", "A", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Self-loop");
    }

    @Test
    @DisplayName("node lookup for a missing id fails loudly")
    void missingNodeLookup() {
        Graph graph = Graph.builder().addNode(new Node("A", "Ashford", 0, 0)).build();

        assertThatThrownBy(() -> graph.node("Z"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
