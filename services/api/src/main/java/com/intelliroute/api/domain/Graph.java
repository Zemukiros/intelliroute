package com.intelliroute.api.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable weighted directed graph backed by an adjacency list.
 *
 * <p>Undirected roads are modelled by adding one edge in each direction.
 * Instances are constructed through {@link Builder} and are safe to share
 * across threads after construction.
 */
public final class Graph {

    private final Map<String, Node> nodesById;
    private final Map<String, List<Edge>> adjacency;
    private final int edgeCount;

    private Graph(Map<String, Node> nodesById, Map<String, List<Edge>> adjacency, int edgeCount) {
        this.nodesById = Collections.unmodifiableMap(nodesById);
        this.adjacency = Collections.unmodifiableMap(adjacency);
        this.edgeCount = edgeCount;
    }

    public boolean containsNode(String id) {
        return nodesById.containsKey(id);
    }

    public Node node(String id) {
        Node node = nodesById.get(id);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node: " + id);
        }
        return node;
    }

    public Collection<Node> nodes() {
        return nodesById.values();
    }

    /** Outgoing edges of a node; empty list when the node has none. */
    public List<Edge> edgesFrom(String nodeId) {
        return adjacency.getOrDefault(nodeId, List.of());
    }

    /**
     * The directed edge from {@code from} to {@code to}.
     *
     * @throws IllegalArgumentException when no such edge exists
     */
    public Edge edgeBetween(String from, String to) {
        return edgesFrom(from).stream()
                .filter(e -> e.to().equals(to))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No edge between '" + from + "' and '" + to + "'"));
    }

    /** All edges of the graph in insertion order. */
    public List<Edge> edges() {
        return adjacency.values().stream().flatMap(List::stream).toList();
    }

    public int nodeCount() {
        return nodesById.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, Node> nodesById = new LinkedHashMap<>();
        private final Map<String, List<Edge>> adjacency = new LinkedHashMap<>();
        private int edgeCount;

        public Builder addNode(Node node) {
            if (nodesById.putIfAbsent(node.id(), node) != null) {
                throw new IllegalArgumentException("Duplicate node id: " + node.id());
            }
            return this;
        }

        /** Adds a one-way plain local road. Both endpoints must already exist. */
        public Builder addEdge(String from, String to, double distanceKm) {
            return addRoad(Edge.local(from, to, distanceKm));
        }

        /** Adds a two-way plain local road: one directed edge in each direction. */
        public Builder addBidirectionalEdge(String a, String b, double distanceKm) {
            return addEdge(a, b, distanceKm).addEdge(b, a, distanceKm);
        }

        /** Adds a one-way road with full metadata. Both endpoints must already exist. */
        public Builder addRoad(Edge edge) {
            requireNode(edge.from());
            requireNode(edge.to());
            adjacency.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge);
            edgeCount++;
            return this;
        }

        /** Adds a two-way road with full metadata (mirrored in both directions). */
        public Builder addBidirectionalRoad(Edge edge) {
            return addRoad(edge).addRoad(edge.reversed());
        }

        private void requireNode(String id) {
            if (!nodesById.containsKey(id)) {
                throw new IllegalArgumentException(
                        "Cannot add edge referencing unknown node: " + id);
            }
        }

        public Graph build() {
            return new Graph(new LinkedHashMap<>(nodesById), deepCopy(adjacency), edgeCount);
        }

        private static Map<String, List<Edge>> deepCopy(Map<String, List<Edge>> source) {
            Map<String, List<Edge>> copy = new LinkedHashMap<>();
            source.forEach((k, v) -> copy.put(k, List.copyOf(v)));
            return copy;
        }
    }
}
