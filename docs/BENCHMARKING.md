# Benchmarking

Reproducible performance measurements for the routing and ranking engines.
Nothing here is estimated — every number comes from an actual recorded run,
and the tooling is committed so anyone can regenerate them.

## Methodology

- **Deterministic networks** — `NetworkGenerator` (Java) builds random
  geometric graphs from a fixed seed (42): a random spanning tree for
  guaranteed connectivity plus extra edges to an average degree of ~4.
  Same seed → identical graph, identical query set.
- **Warm-up before measurement** — JIT/interpreter warm-up passes run
  before any timed sample.
- **Per-call wall-clock timing** — `System.nanoTime()` (Java),
  `time.perf_counter_ns()` (Python); we report average, median, and p95.
- **Generated data is benchmark-only** — the application's sample network
  is hand-authored; nothing random happens at application startup.

Run them yourself:

```bash
# Java routing benchmarks (also runnable framework-free with javac)
cd services/api
mvn -q compile exec:java -Dexec.mainClass=com.intelliroute.api.bench.BenchmarkRunner

# Python ranking benchmark
cd services/ai-service
python3 scripts/benchmark_ranking.py
```

CI also prints the ranking benchmark and a live end-to-end recommend
latency measurement in each run's summary.

## Recorded results — 2026-08-01

Environment: Linux x86_64 cloud container, 2 cores, OpenJDK 21.0.10
(2 GB heap), Python 3.11.15. Single-threaded. Seed 42, avg degree 4, K=3.

### Dijkstra shortest path

| Network | Runs | Avg | Median | P95 |
|---|---|---|---|---|
| 100 nodes / 400 directed edges | 2,000 | 88 µs | 90 µs | 154 µs |
| 1,000 nodes / 4,000 edges | 1,000 | 754 µs | 597 µs | 1.69 ms |
| 5,000 nodes / 20,000 edges | 300 | 4.05 ms | 3.90 ms | 9.78 ms |

### Yen's K=3 alternatives

| Network | Runs | Avg | Median | P95 |
|---|---|---|---|---|
| 100 nodes | 300 | 1.11 ms | 0.84 ms | 2.58 ms |
| 1,000 nodes | 150 | 7.68 ms | 7.47 ms | 13.5 ms |
| 5,000 nodes | 50 | 68.1 ms | 66.0 ms | 108 ms |

Yen's cost is roughly (path length × K) Dijkstra spur searches, which
matches the observed ~10–17× multiple over a single Dijkstra query.

### Preference ranking (Python, parse + score, 3–5 candidates)

| Runs | Avg | Median | P95 |
|---|---|---|---|
| 10,000 | 91 µs | 86 µs | 138 µs |

## About the Step 1 number (4.92 µs)

The originally recorded **4.92 µs average Dijkstra search** (2026-08-01,
20,000 runs post-warmup) was measured on the **small 10-node / 30-directed-
edge Step 1 sample network** — it remains valid for that network and is
kept for historical accuracy, but it is *not* comparable to the generated-
network results above, which are the numbers to quote for scaling behaviour.

Machine dependence: all numbers above are from the 2-core cloud container
noted; expect different (often faster) absolute values on modern desktop
hardware. The tooling records its environment with every run.
