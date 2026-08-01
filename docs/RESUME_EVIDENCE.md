# Résumé Evidence Tracker

Purpose: résumé and portfolio claims about IntelliRoute may only be written
from rows marked **Verified** here. No final résumé bullets exist yet — they
are drafted in a later phase from verified rows only.

Legend: **Verified** = implemented and confirmed by tests/measurements listed
in the Evidence column. **Implemented** = code exists, verification pending.
**Planned** = not built yet; must not appear on the résumé.

| # | Claim (candidate) | Status | Evidence |
|---|---|---|---|
| 1 | Designed and implemented Dijkstra's shortest-path engine in Java 21 with binary-heap priority queue, lazy deletion, and early termination | **Verified** | 8/8 assertions in local JDK harness (paths, distances, unreachable, invalid input); `DijkstraPathFinderTest` (11 tests) in CI |
| 2 | Measured mean shortest-path query latency of ~5 µs on the 10-node/30-directed-edge sample network (20,000-run benchmark, post-warmup) | **Verified** | Local harness benchmark 2026-08-01: 4.92 µs avg; re-runnable, machine-dependent |
| 3 | Built a Spring Boot 3 / Java 21 REST API with bean validation, typed domain errors, and a consistent JSON problem contract | **Verified** | CI run [#30706883118](https://github.com/Zemukiros/intelliroute/actions/runs/30706883118) green (2026-08-01): `mvn verify` (24 JUnit tests incl. 7 MockMvc) + live boot-and-curl smoke test (200 with distance 14.0, 422 unreachable) |
| 4 | Immutable, builder-validated graph domain model safe for concurrent request handling | **Verified** (design + tests) | `GraphTest` (6 tests); immutability by construction (`List.copyOf`, unmodifiable maps) |
| 5 | Developed an interactive Next.js 15/TypeScript frontend with SVG network visualization and live route highlighting | **Verified** (build quality) | `npm run lint` 0 warnings + `npm run build` passing locally and in CI run #30706883118; manual browser verification pending backend co-run |
| 6 | Designed a polyglot microservice architecture (Java routing engine, Python AI service, TypeScript frontend) with Docker Compose orchestration | **Implemented** | Compose file + three Dockerfiles exist; full-stack container run not yet executed |
| 7 | Established a provider-abstracted AI re-ranking service (FastAPI) with deterministic mock provider | **Verified** | Live TestClient check: `/health` 200, `/rank` 200 with correct ordering (2026-08-01) |
| 8 | CI pipeline (GitHub Actions): Maven verify, live API smoke test, frontend lint+build, AI-service sanity check | **Verified** | First run green on push: all 3 jobs passed (API 33s, Web 39s, AI 10s), run #30706883118, 2026-08-01 |
| 9 | Implemented Yen's K-shortest loopless-paths algorithm over an exclusion-capable Dijkstra core, with uniqueness, looplessness, closed-road handling, and deterministic ordering | **Verified** | Step 2 JDK harness: 23/23 assertions (known A→F alternatives 14/17/18 km, closures, determinism, metrics); `YenKShortestPathsTest` in CI |
| 10 | Modeled road metadata (types with typical speeds, tolls, safety/scenic scores, closures) with distance-weighted route aggregation | **Verified** | Exact-value assertions in harness + `YenKShortestPathsTest`/`GraphTest` (e.g. A-B-D-F: 11.8 min, 5.50 toll, 70.6% highway, safety 8.41) |
| 11 | Built a local deterministic natural-language preference-ranking service (Python/FastAPI): synonym/phrase parsing, combined preferences, weight normalization, min-max utility scoring, explanations, confidence reporting, safe fallbacks | **Verified** | 54/54 pytest (2026-08-01) incl. exact winner assertions per preference; benchmark 91 µs avg per parse+rank call (10k runs) |
| 12 | Integrated Spring Boot with the ranking service: configurable timeouts, health indicator, structured error translation, graceful local fallback | **Implemented** → Verified on green CI | `RankingClientTest` (mocked HTTP), `RoutingServiceFallbackTest` (dead-port fallback), CI live fallback drill (service killed mid-run) |
| 13 | Benchmarked routing on deterministic generated networks: Dijkstra 88 µs avg @100 nodes / 754 µs @1k / 4.05 ms @5k; Yen K=3 1.1 ms / 7.7 ms / 68 ms (2-core container, seed 42, warm-up, avg/median/p95) | **Verified** | Recorded run 2026-08-01, methodology + tooling in docs/BENCHMARKING.md; re-runnable, machine-dependent |
| 14 | Built a route-comparison UI with natural-language input, ranked cards, badges, selection-driven graph highlighting, and offline/fallback states | **Verified** (build + component tests) | 9/9 Vitest component tests, ESLint 0 warnings, production build passing (2026-08-01); browser co-run pending user machine |
| 15 | Extended CI to live end-to-end verification: real jar + real ranking service, preference scenarios, fallback drill, Docker Compose build & smoke | **Implemented** → Verified on first green run of `feature/route-intelligence` | `.github/workflows/ci.yml` |
| 16 | Optional LLM/Ollama ranking provider | **Planned** | Cost-gated; must not appear on résumé |
| 17 | PostgreSQL-backed network persistence | **Planned** | Milestone 3 |
| 18 | AWS/cloud deployment | **Planned** | Milestone 4 |

## Measurement log

- **2026-08-01 (Step 2)** — Routing benchmarks on seeded generated networks (2-core Linux container, OpenJDK 21, single thread, seed 42): Dijkstra avg 88.2 µs / median 90.1 / p95 153.9 @100 nodes; 754.3 / 597.3 / 1691.4 µs @1,000 nodes; 4.05 / 3.90 / 9.78 ms @5,000 nodes. Yen K=3: 1.11 / 0.84 / 2.58 ms @100; 7.68 / 7.47 / 13.5 ms @1,000; 68.1 / 66.0 / 107.6 ms @5,000. Python ranking (parse+rank, 3–5 candidates, 10,000 runs): avg 91.0 µs / median 86.0 / p95 137.5. Methodology: docs/BENCHMARKING.md. The Step 1 figure of 4.92 µs below was measured on the small 10-node sample network and is retained for historical accuracy only.
- **2026-08-01 (Step 2)** — Python suite 54/54 passed; frontend 9/9 component tests, lint clean, production build passing; Step 2 JDK harness 23/23.

- **2026-08-01** — First CI run on GitHub ([#30706883118](https://github.com/Zemukiros/intelliroute/actions/runs/30706883118)): all 3 jobs green. `mvn verify` full suite passed; live smoke test confirmed `POST /api/routes/calculate` A→F = 14.0 km (200) and A→J unreachable (422) against the booted jar.

- **2026-08-01** — Dijkstra micro-benchmark (sandbox, JDK 21, single thread): 20,000 A→F searches after 5,000-run warmup → **4.92 µs average** per full search on the 10-node sample network. Command preserved in project history; regenerate with the CI benchmark job planned for milestone 2.
- **2026-08-01** — Frontend production build: compiled successfully; route `/` first-load JS ≈ 105 kB.
- **2026-08-01** — ESLint (`next/core-web-vitals` + `next/typescript`): 0 errors, 0 warnings.
