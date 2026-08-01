package com.intelliroute.api.engine;

import static com.intelliroute.api.domain.RoadType.ARTERIAL;
import static com.intelliroute.api.domain.RoadType.HIGHWAY;
import static com.intelliroute.api.domain.RoadType.LOCAL;
import static com.intelliroute.api.domain.RoadType.SCENIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.intelliroute.api.domain.Edge;
import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.domain.Node;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class YenKShortestPathsTest {

    private YenKShortestPaths yen;
    private DijkstraPathFinder dijkstra;
    private Graph graph;

    @BeforeEach
    void setUp() {
        dijkstra = new DijkstraPathFinder();
        yen = new YenKShortestPaths(dijkstra);
        // Mirror of the sample network including metadata and the closed B–E road.
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
                .addBidirectionalRoad(Edge.of("A", "B", 5, ARTERIAL, 0, 7, 4, true))
                .addBidirectionalRoad(Edge.of("A", "C", 4, LOCAL, 0, 6, 6, true))
                .addBidirectionalRoad(Edge.of("B", "C", 6, LOCAL, 0, 6, 5, true))
                .addBidirectionalRoad(Edge.of("B", "D", 7, HIGHWAY, 2.50, 9, 2, true))
                .addBidirectionalRoad(Edge.of("B", "G", 6, HIGHWAY, 0, 9, 3, true))
                .addBidirectionalRoad(Edge.of("C", "D", 5, LOCAL, 0, 5, 5, true))
                .addBidirectionalRoad(Edge.of("C", "E", 8, SCENIC, 0, 7, 9, true))
                .addBidirectionalRoad(Edge.of("D", "E", 4, LOCAL, 0, 6, 6, true))
                .addBidirectionalRoad(Edge.of("D", "F", 5, HIGHWAY, 3.00, 9, 2, true))
                .addBidirectionalRoad(Edge.of("D", "G", 5, ARTERIAL, 0, 7, 4, true))
                .addBidirectionalRoad(Edge.of("E", "F", 6, SCENIC, 0, 7, 9, true))
                .addBidirectionalRoad(Edge.of("E", "I", 7, SCENIC, 0, 6, 8, true))
                .addBidirectionalRoad(Edge.of("F", "H", 4, ARTERIAL, 0, 8, 5, true))
                .addBidirectionalRoad(Edge.of("F", "I", 5, LOCAL, 0, 6, 6, true))
                .addBidirectionalRoad(Edge.of("G", "H", 6, HIGHWAY, 0, 9, 3, true))
                .addBidirectionalRoad(Edge.of("B", "E", 9, LOCAL, 0, 5, 5, false))
                .build();
    }

    @Nested
    @DisplayName("multiple route generation")
    class MultipleRoutes {

        @Test
        @DisplayName("A -> F yields the three known alternatives in distance order")
        void threeAlternativesForAtoF() {
            var result = yen.findKShortestPaths(graph, "A", "F", 3);

            assertThat(result.paths()).hasSize(3);
            assertThat(result.paths().get(0).path()).containsExactly("A", "C", "D", "F");
            assertThat(result.paths().get(0).distanceKm()).isEqualTo(14.0);
            assertThat(result.paths().get(1).path()).containsExactly("A", "B", "D", "F");
            assertThat(result.paths().get(1).distanceKm()).isEqualTo(17.0);
            assertThat(result.paths().get(2).path()).containsExactly("A", "C", "E", "F");
            assertThat(result.paths().get(2).distanceKm()).isEqualTo(18.0);
        }

        @Test
        @DisplayName("first route always equals the plain Dijkstra shortest path")
        void firstRouteIsShortest() {
            var result = yen.findKShortestPaths(graph, "A", "H", 3);
            var shortest = dijkstra.findShortestPath(graph, "A", "H");

            assertThat(result.paths().get(0).path()).isEqualTo(shortest.path());
            assertThat(result.paths().get(0).distanceKm())
                    .isEqualTo(shortest.totalDistanceKm());
        }

        @Test
        @DisplayName("routes are unique")
        void routesAreUnique() {
            var result = yen.findKShortestPaths(graph, "A", "F", 5);
            var distinct = new HashSet<>(result.paths().stream()
                    .map(YenKShortestPaths.CandidatePath::path).toList());

            assertThat(distinct).hasSameSizeAs(result.paths());
        }

        @Test
        @DisplayName("routes never contain loops")
        void routesAreLoopless() {
            var result = yen.findKShortestPaths(graph, "A", "I", 5);

            for (var candidate : result.paths()) {
                assertThat(candidate.path())
                        .as("path %s must not revisit nodes", candidate.path())
                        .doesNotHaveDuplicates();
            }
        }

        @Test
        @DisplayName("deterministic: repeated searches give identical results")
        void deterministicOrdering() {
            var first = yen.findKShortestPaths(graph, "A", "F", 4);
            var second = yen.findKShortestPaths(graph, "A", "F", 4);

            assertThat(first.paths()).isEqualTo(second.paths());
        }

        @Test
        @DisplayName("search statistics are reported")
        void searchStatistics() {
            var result = yen.findKShortestPaths(graph, "A", "F", 3);

            assertThat(result.dijkstraInvocations()).isGreaterThan(1);
            assertThat(result.totalVisitedNodes()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("closed roads are never used in any candidate")
        void closedRoadsExcluded() {
            var result = yen.findKShortestPaths(graph, "B", "E", 5);

            for (var candidate : result.paths()) {
                List<String> path = candidate.path();
                for (int i = 0; i < path.size() - 1; i++) {
                    boolean usesClosed =
                            (path.get(i).equals("B") && path.get(i + 1).equals("E"))
                            || (path.get(i).equals("E") && path.get(i + 1).equals("B"));
                    assertThat(usesClosed)
                            .as("candidate %s must not traverse closed B–E", path)
                            .isFalse();
                }
            }
            assertThat(result.paths().get(0).path()).containsExactly("B", "D", "E");
        }

        @Test
        @DisplayName("returns fewer routes when the graph has fewer distinct paths")
        void fewerRoutesThanRequested() {
            Graph tiny = Graph.builder()
                    .addNode(new Node("X", "X-Town", 0, 0))
                    .addNode(new Node("Y", "Y-Ville", 0, 0))
                    .addBidirectionalEdge("X", "Y", 3)
                    .build();

            var result = yen.findKShortestPaths(tiny, "X", "Y", 5);

            assertThat(result.paths()).hasSize(1);
        }

        @Test
        @DisplayName("unreachable destination yields an empty result")
        void unreachableDestination() {
            var result = yen.findKShortestPaths(graph, "A", "J", 3);

            assertThat(result.paths()).isEmpty();
        }

        @Test
        @DisplayName("k = 1 degenerates to a single Dijkstra search")
        void kOfOne() {
            var result = yen.findKShortestPaths(graph, "A", "F", 1);

            assertThat(result.paths()).hasSize(1);
            assertThat(result.dijkstraInvocations()).isEqualTo(1);
        }

        @Test
        @DisplayName("invalid k and unknown nodes are rejected")
        void invalidInput() {
            assertThatThrownBy(() -> yen.findKShortestPaths(graph, "A", "F", 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> yen.findKShortestPaths(graph, "Z", "F", 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("route metadata aggregation")
    class MetadataAggregation {

        @Test
        @DisplayName("candidate metrics aggregate distance, time, toll, and shares")
        void aggregatesMetrics() {
            RouteCandidate candidate =
                    RouteCandidate.fromPath("route-2", graph, List.of("A", "B", "D", "F"));

            assertThat(candidate.totalDistanceKm()).isEqualTo(17.0);
            assertThat(candidate.travelTimeMinutes()).isEqualTo(11.8);
            assertThat(candidate.tollCost()).isEqualTo(5.50);
            assertThat(candidate.highwayPercentage()).isEqualTo(70.6);
            assertThat(candidate.safetyScore()).isEqualTo(8.41);
        }

        @Test
        @DisplayName("toll-free scenic candidate reports zero toll and highway share")
        void scenicCandidate() {
            RouteCandidate candidate =
                    RouteCandidate.fromPath("route-3", graph, List.of("A", "C", "E", "F"));

            assertThat(candidate.tollCost()).isZero();
            assertThat(candidate.highwayPercentage()).isZero();
            assertThat(candidate.scenicScore()).isEqualTo(8.33);
        }
    }
}
