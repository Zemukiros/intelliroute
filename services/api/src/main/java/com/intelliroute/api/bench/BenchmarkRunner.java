package com.intelliroute.api.bench;

import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.engine.DijkstraPathFinder;
import com.intelliroute.api.engine.PathResult;
import com.intelliroute.api.engine.YenKShortestPaths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Reproducible routing benchmarks on deterministic generated networks.
 *
 * <p>Methodology: fixed seed (42), fixed query sets, JIT warm-up before every
 * measurement, per-query wall-clock timing via {@link System#nanoTime()},
 * reporting average / median / p95. Framework-free by design so it runs with
 * a bare JDK.
 *
 * <p>Run with Maven:
 * <pre>mvn -q compile exec:java -Dexec.mainClass=com.intelliroute.api.bench.BenchmarkRunner</pre>
 * or compile the {@code domain}, {@code engine}, and {@code bench} packages
 * with {@code javac} and run directly.
 */
public final class BenchmarkRunner {

    private static final long SEED = 42;
    private static final double AVG_DEGREE = 4.0;
    private static final int K = 3;

    private record SizeConfig(int nodes, int warmup, int runs, int yenWarmup, int yenRuns) {}

    public static void main(String[] args) {
        List<SizeConfig> configs = List.of(
                new SizeConfig(100, 500, 2_000, 50, 300),
                new SizeConfig(1_000, 200, 1_000, 20, 150),
                new SizeConfig(5_000, 50, 300, 10, 50));

        System.out.println("# IntelliRoute routing benchmarks");
        System.out.printf("environment: java=%s, os=%s %s, cores=%d, maxHeapMB=%d%n",
                System.getProperty("java.version"),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
        System.out.printf("seed=%d, avgDegree=%.1f, k=%d%n%n", SEED, AVG_DEGREE, K);

        DijkstraPathFinder dijkstra = new DijkstraPathFinder();
        YenKShortestPaths yen = new YenKShortestPaths(dijkstra);

        for (SizeConfig config : configs) {
            Graph graph = NetworkGenerator.generate(config.nodes(), AVG_DEGREE, SEED);
            Random queryRng = new Random(SEED + 1);
            System.out.printf("## %d nodes, %d directed edges%n",
                    graph.nodeCount(), graph.edgeCount());

            // --- Dijkstra shortest path
            String[][] queries = randomPairs(queryRng, config.nodes(),
                    Math.max(config.runs(), config.warmup()));
            for (int i = 0; i < config.warmup(); i++) {
                blackhole(dijkstra.findShortestPath(graph,
                        queries[i % queries.length][0], queries[i % queries.length][1]));
            }
            long[] samples = new long[config.runs()];
            for (int i = 0; i < config.runs(); i++) {
                long start = System.nanoTime();
                blackhole(dijkstra.findShortestPath(graph, queries[i][0], queries[i][1]));
                samples[i] = System.nanoTime() - start;
            }
            report("dijkstra shortest path", samples, config.runs());

            // --- Yen K=3 alternatives
            String[][] yenQueries = randomPairs(queryRng, config.nodes(),
                    Math.max(config.yenRuns(), config.yenWarmup()));
            for (int i = 0; i < config.yenWarmup(); i++) {
                blackhole(yen.findKShortestPaths(graph,
                        yenQueries[i % yenQueries.length][0],
                        yenQueries[i % yenQueries.length][1], K));
            }
            long[] yenSamples = new long[config.yenRuns()];
            for (int i = 0; i < config.yenRuns(); i++) {
                long start = System.nanoTime();
                blackhole(yen.findKShortestPaths(graph,
                        yenQueries[i][0], yenQueries[i][1], K));
                yenSamples[i] = System.nanoTime() - start;
            }
            report("yen k=3 alternatives", yenSamples, config.yenRuns());
            System.out.println();
        }
    }

    private static String[][] randomPairs(Random rng, int nodeCount, int count) {
        String[][] pairs = new String[count][2];
        for (int i = 0; i < count; i++) {
            int a = rng.nextInt(nodeCount);
            int b;
            do {
                b = rng.nextInt(nodeCount);
            } while (b == a);
            pairs[i][0] = "N" + a;
            pairs[i][1] = "N" + b;
        }
        return pairs;
    }

    private static void report(String label, long[] samplesNanos, int runs) {
        long[] sorted = samplesNanos.clone();
        Arrays.sort(sorted);
        double avgMicros = Arrays.stream(samplesNanos).average().orElse(0) / 1_000.0;
        double medianMicros = sorted[sorted.length / 2] / 1_000.0;
        double p95Micros = sorted[(int) Math.floor(sorted.length * 0.95) - 1] / 1_000.0;
        System.out.printf(
                "%-24s runs=%-5d avg=%.1f us  median=%.1f us  p95=%.1f us%n",
                label, runs, avgMicros, medianMicros, p95Micros);
    }

    // Prevents the JIT from eliminating the measured work.
    private static final List<Object> SINK = new ArrayList<>(1);

    private static void blackhole(Object value) {
        if (value instanceof PathResult p && p.visitedNodes() < 0) {
            SINK.add(value);
        }
        if (value instanceof YenKShortestPaths.SearchResult s && s.dijkstraInvocations() < 0) {
            SINK.add(value);
        }
    }

    private BenchmarkRunner() {}
}
