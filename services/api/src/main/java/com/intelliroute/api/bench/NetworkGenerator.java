package com.intelliroute.api.bench;

import com.intelliroute.api.domain.Edge;
import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.domain.Node;
import com.intelliroute.api.domain.RoadType;
import java.util.Random;

/**
 * Deterministic random-network generator for benchmarking.
 *
 * <p>Given the same {@code nodeCount} and {@code seed}, always produces the
 * identical graph, so benchmark runs are reproducible. Connectivity is
 * guaranteed by attaching every node to a random earlier node (a random
 * spanning tree), then extra edges are added until the target average degree
 * is reached. Used ONLY by the benchmark utility — the application's sample
 * network is hand-authored and never randomized.
 */
public final class NetworkGenerator {

    private static final RoadType[] TYPES = RoadType.values();

    private NetworkGenerator() {}

    /** Generates a connected network with ~{@code avgDegree} edges per node. */
    public static Graph generate(int nodeCount, double avgDegree, long seed) {
        Random rng = new Random(seed);
        Graph.Builder builder = Graph.builder();

        for (int i = 0; i < nodeCount; i++) {
            builder.addNode(new Node("N" + i, "Town " + i,
                    rng.nextInt(10_000), rng.nextInt(10_000)));
        }

        // Random spanning tree: guarantees connectivity.
        for (int i = 1; i < nodeCount; i++) {
            int target = rng.nextInt(i);
            builder.addBidirectionalRoad(randomRoad(rng, "N" + i, "N" + target));
        }

        // Top up to the target average degree (each two-way road adds 2 directed edges).
        long targetDirectedEdges = Math.round(nodeCount * avgDegree);
        long currentDirectedEdges = 2L * (nodeCount - 1);
        int attempts = 0;
        int maxAttempts = nodeCount * 100;
        java.util.Set<String> used = new java.util.HashSet<>();
        while (currentDirectedEdges < targetDirectedEdges && attempts++ < maxAttempts) {
            int a = rng.nextInt(nodeCount);
            int b = rng.nextInt(nodeCount);
            if (a == b) {
                continue;
            }
            String key = Math.min(a, b) + ":" + Math.max(a, b);
            if (!used.add(key)) {
                continue;
            }
            builder.addBidirectionalRoad(randomRoad(rng, "N" + a, "N" + b));
            currentDirectedEdges += 2;
        }
        return builder.build();
    }

    private static Edge randomRoad(Random rng, String from, String to) {
        RoadType type = TYPES[rng.nextInt(TYPES.length)];
        double distance = 1 + rng.nextInt(15);
        double toll = type == RoadType.HIGHWAY && rng.nextInt(3) == 0
                ? 1 + rng.nextInt(4) : 0;
        double safety = 3 + rng.nextInt(8);
        double scenic = rng.nextInt(11);
        return Edge.of(from, to, distance, type, toll, safety, scenic, true);
    }
}
