package com.intelliroute.api.engine;

import com.intelliroute.api.domain.Edge;
import com.intelliroute.api.domain.Graph;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Dijkstra's single-source shortest-path algorithm over a weighted graph
 * with non-negative edge weights.
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>Uses a binary-heap {@link PriorityQueue} with lazy deletion
 *       (stale queue entries are skipped when polled), giving
 *       O((V + E) log V) time and O(V) space.</li>
 *   <li>Terminates early as soon as the destination is settled — the
 *       remaining frontier cannot improve on a settled node's distance.</li>
 *   <li>Closed roads ({@code edge.open() == false}) are never traversed.</li>
 *   <li>Supports excluding specific nodes and directed edges, which is the
 *       primitive Yen's k-shortest-paths algorithm builds on.</li>
 *   <li>Stateless and therefore thread-safe; one instance serves all
 *       concurrent requests.</li>
 * </ul>
 */
@Component
public class DijkstraPathFinder {

    private record QueueEntry(String nodeId, double distance) {}

    /** Directed-edge exclusion key. */
    public static String edgeKey(String from, String to) {
        return from + ">" + to;
    }

    /**
     * Computes the shortest path between two nodes.
     *
     * @throws IllegalArgumentException when either node is not in the graph
     */
    public PathResult findShortestPath(Graph graph, String originId, String destinationId) {
        return findShortestPath(graph, originId, destinationId, Set.of(), Set.of());
    }

    /**
     * Computes the shortest path while treating the given nodes and directed
     * edges (keys from {@link #edgeKey}) as removed from the graph.
     *
     * @return the shortest path, or {@link PathResult#unreachable(int)}
     *         when no path exists under the given exclusions
     */
    public PathResult findShortestPath(Graph graph, String originId, String destinationId,
                                       Set<String> excludedNodes, Set<String> excludedEdges) {
        requireNode(graph, originId, "origin");
        requireNode(graph, destinationId, "destination");

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> settled = new HashSet<>();
        PriorityQueue<QueueEntry> frontier =
                new PriorityQueue<>((a, b) -> Double.compare(a.distance(), b.distance()));

        distances.put(originId, 0.0);
        frontier.add(new QueueEntry(originId, 0.0));

        while (!frontier.isEmpty()) {
            QueueEntry current = frontier.poll();

            // Lazy deletion: skip entries superseded by a shorter distance.
            if (settled.contains(current.nodeId())) {
                continue;
            }
            settled.add(current.nodeId());

            // Early exit: once the destination is settled its distance is final.
            if (current.nodeId().equals(destinationId)) {
                return new PathResult(
                        reconstructPath(previous, originId, destinationId),
                        distances.get(destinationId),
                        settled.size());
            }

            for (Edge edge : graph.edgesFrom(current.nodeId())) {
                if (!edge.open()
                        || settled.contains(edge.to())
                        || excludedNodes.contains(edge.to())
                        || excludedEdges.contains(edgeKey(edge.from(), edge.to()))) {
                    continue;
                }
                double candidate = distances.get(current.nodeId()) + edge.distanceKm();
                double known = distances.getOrDefault(edge.to(), Double.POSITIVE_INFINITY);
                if (candidate < known) {
                    distances.put(edge.to(), candidate);
                    previous.put(edge.to(), current.nodeId());
                    frontier.add(new QueueEntry(edge.to(), candidate));
                }
            }
        }

        return PathResult.unreachable(settled.size());
    }

    private static List<String> reconstructPath(
            Map<String, String> previous, String originId, String destinationId) {
        Deque<String> path = new ArrayDeque<>();
        String cursor = destinationId;
        while (cursor != null) {
            path.addFirst(cursor);
            if (cursor.equals(originId)) {
                break;
            }
            cursor = previous.get(cursor);
        }
        return List.copyOf(path);
    }

    private static void requireNode(Graph graph, String nodeId, String role) {
        if (!graph.containsNode(nodeId)) {
            throw new IllegalArgumentException(
                    "Unknown " + role + " node: '" + nodeId + "'");
        }
    }
}
