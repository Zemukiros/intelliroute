package com.intelliroute.api.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.domain.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DijkstraPathFinderTest {

    private DijkstraPathFinder pathFinder;
    private Graph graph;

    @BeforeEach
    void setUp() {
        pathFinder = new DijkstraPathFinder();
        // Mirror of the sample network topology, including the
        // intentionally disconnected node "J".
        graph = Graph.builder()
                .addNode(new Node("A", "Ashford", 0, 0))
                .addNode(new Node("B", "Brookhaven", 0, 0))
                .addNode(new Node("C", "Cedarville", 0, 0))
                .addNode(new Node("D", "Dunmore", 0, 0))
                .addNode(new Node("E", "Eastvale", 0, 0))
                .addNode(new Node("F", "Fairmont", 0, 0))
                .addNode(new Node("G", "Glenrock", 0, 0))
                .addNode(new Node("H", "Harborview", 0, 0))
                .addNode(new Node("I", "Ironbridge", 0, 0))
                .addNode(new Node("J", "Juniper Isle", 0, 0))
                .addBidirectionalEdge("A", "B", 5)
                .addBidirectionalEdge("A", "C", 4)
                .addBidirectionalEdge("B", "C", 6)
                .addBidirectionalEdge("B", "D", 7)
                .addBidirectionalEdge("B", "G", 6)
                .addBidirectionalEdge("C", "D", 5)
                .addBidirectionalEdge("C", "E", 8)
                .addBidirectionalEdge("D", "E", 4)
                .addBidirectionalEdge("D", "F", 5)
                .addBidirectionalEdge("D", "G", 5)
                .addBidirectionalEdge("E", "F", 6)
                .addBidirectionalEdge("E", "I", 7)
                .addBidirectionalEdge("F", "H", 4)
                .addBidirectionalEdge("F", "I", 5)
                .addBidirectionalEdge("G", "H", 6)
                .build();
    }

    @Nested
    @DisplayName("successful routes")
    class SuccessfulRoutes {

        @Test
        @DisplayName("finds the shortest path A -> F through C and D")
        void shortestPathAtoF() {
            PathResult result = pathFinder.findShortestPath(graph, "A", "F");

            assertThat(result.isReachable()).isTrue();
            assertThat(result.path()).containsExactly("A", "C", "D", "F");
            assertThat(result.totalDistanceKm()).isEqualTo(14.0);
        }

        @Test
        @DisplayName("prefers the cheaper of two competing branches (A -> H)")
        void shortestPathAtoH() {
            // Via F: A-C-D-F-H = 18.  Via G: A-B-G-H = 17.
            PathResult result = pathFinder.findShortestPath(graph, "A", "H");

            assertThat(result.path()).containsExactly("A", "B", "G", "H");
            assertThat(result.totalDistanceKm()).isEqualTo(17.0);
        }

        @Test
        @DisplayName("works in the reverse direction on two-way roads")
        void shortestPathFtoA() {
            PathResult result = pathFinder.findShortestPath(graph, "F", "A");

            assertThat(result.path()).containsExactly("F", "D", "C", "A");
            assertThat(result.totalDistanceKm()).isEqualTo(14.0);
        }

        @Test
        @DisplayName("single-hop route uses the direct edge")
        void directNeighbor() {
            PathResult result = pathFinder.findShortestPath(graph, "F", "H");

            assertThat(result.path()).containsExactly("F", "H");
            assertThat(result.totalDistanceKm()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("visited-node count is positive and bounded by graph size")
        void visitedNodesBounded() {
            PathResult result = pathFinder.findShortestPath(graph, "A", "F");

            assertThat(result.visitedNodes()).isPositive();
            assertThat(result.visitedNodes()).isLessThanOrEqualTo(graph.nodeCount());
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("origin equals destination yields a zero-length path")
        void sameOriginAndDestination() {
            PathResult result = pathFinder.findShortestPath(graph, "A", "A");

            assertThat(result.path()).containsExactly("A");
            assertThat(result.totalDistanceKm()).isEqualTo(0.0);
            assertThat(result.visitedNodes()).isEqualTo(1);
        }

        @Test
        @DisplayName("disconnected destination is reported unreachable")
        void unreachableDestination() {
            PathResult result = pathFinder.findShortestPath(graph, "A", "J");

            assertThat(result.isReachable()).isFalse();
            assertThat(result.path()).isEmpty();
            assertThat(result.totalDistanceKm()).isEqualTo(Double.POSITIVE_INFINITY);
            // The search must have exhausted the whole mainland component.
            assertThat(result.visitedNodes()).isEqualTo(9);
        }

        @Test
        @DisplayName("disconnected origin is reported unreachable after visiting itself")
        void unreachableOrigin() {
            PathResult result = pathFinder.findShortestPath(graph, "J", "A");

            assertThat(result.isReachable()).isFalse();
            assertThat(result.visitedNodes()).isEqualTo(1);
        }

        @Test
        @DisplayName("unknown origin is rejected")
        void unknownOrigin() {
            assertThatThrownBy(() -> pathFinder.findShortestPath(graph, "Z", "A"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("origin");
        }

        @Test
        @DisplayName("unknown destination is rejected")
        void unknownDestination() {
            assertThatThrownBy(() -> pathFinder.findShortestPath(graph, "A", "Z"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("destination");
        }

        @Test
        @DisplayName("trivial two-node graph resolves the only path")
        void minimalGraph() {
            Graph tiny = Graph.builder()
                    .addNode(new Node("X", "X-Town", 0, 0))
                    .addNode(new Node("Y", "Y-Ville", 0, 0))
                    .addBidirectionalEdge("X", "Y", 3)
                    .build();

            PathResult result = pathFinder.findShortestPath(tiny, "X", "Y");

            assertThat(result.path()).containsExactly("X", "Y");
            assertThat(result.totalDistanceKm()).isEqualTo(3.0);
        }
    }
}
