# Step 2 Development Log — Multi-Route Intelligence & Preference-Based Ranking

Date: 2026-08-01 · Branch: `feature/route-intelligence` · Base tag: `v0.1.0-step1`

## Goal

Upgrade IntelliRoute from a single shortest-path calculator into an
intelligent route-comparison platform: multiple candidate routes, real road
tradeoffs, natural-language preferences, explainable deterministic ranking,
side-by-side comparison UI, tests, and reproducible benchmarks — at $0 cost.

## What was built, in order

1. **Git checkpoint** — verified clean tree and remote state, tagged Step 1
   as `v0.1.0-step1`, branched `feature/route-intelligence`.
2. **Domain expansion** — `RoadType` enum (typical speeds), `Edge` extended
   with travel time, tolls, safety/scenic scores, and open/closed status;
   builder validation for every field; sample network enriched with
   documented deterministic metadata, a closed road (B–E), and three
   genuine A→F tradeoff routes (14 km tolled / 17 km fast-safe highway /
   18 km toll-free scenic).
3. **Engine** — Dijkstra gained closed-road awareness and node/edge
   exclusions; Yen's K-shortest loopless paths built on top (unique,
   loopless, deterministic, with search statistics); `RouteCandidate`
   aggregates distance-weighted metrics. Verified immediately with a
   framework-free JDK harness: **23/23 assertions**.
4. **Ranking service** — replaced the Step 1 mock with the local
   deterministic preference-ranking provider (`local-deterministic-v1`):
   phrase-then-synonym parsing, combined preferences, weight
   normalization, confidence + unrecognized-term reporting, min-max
   utility scoring, deterministic tie-breaks, templated explanations,
   balanced-default fallback. **54/54 pytest** locally.
5. **Integration** — `RankingClient` (configurable timeouts, one failure
   exception type), ranking health indicator, `RoutingService`
   orchestration with measured routing/ranking times and the
   `java-local-fallback` degradation path; endpoints
   `/api/routes/alternatives` and `/api/routes/recommend`.
6. **Frontend** — preference textarea with example chips, max-routes
   selector, ranked comparison cards (Recommended/Shortest badges, metric
   grids, explanation on the winner), selection-driven graph highlighting,
   dashed closed roads, offline/fallback/validation states, keyboard
   accessibility. **9/9 Vitest tests**, lint clean, build passing.
7. **Benchmarks** — deterministic network generator + benchmark runner
   (Java) and ranking benchmark (Python); recorded 100/1k/5k-node results
   in docs/BENCHMARKING.md.
8. **CI** — live e2e job (real jar + real ranking service: preference
   scenarios, 422 case, measured latency, kill-service fallback drill),
   Python test+benchmark job, frontend test step, Docker Compose
   build-and-smoke job.
9. **Docs** — ROUTE_RANKING, TESTING, BENCHMARKING, CHANGELOG, this log;
   README/ARCHITECTURE/API/ROADMAP/TECHNICAL_DECISIONS (TD-008…TD-011)
   and RESUME_EVIDENCE updated.

## Environment constraint (unchanged from Step 1)

The cloud build sandbox blocks Maven Central, so `mvn test` cannot run
there. Per TD-007, the framework-free algorithm core was verified locally
with a JDK-only harness, and GitHub Actions remains the authoritative
environment for the full Maven suite, live e2e, and Docker builds.

## Verification summary (local, 2026-08-01)

| Check | Result |
|---|---|
| JDK harness (domain + Dijkstra + Yen + metrics) | 23/23 passed |
| Python suite | 54/54 passed |
| Frontend component tests | 9/9 passed |
| Frontend lint / production build | 0 warnings / passing |
| Java benchmarks (100/1k/5k nodes) | recorded |
| Python ranking benchmark | recorded (91 µs avg) |
| Secret scan | clean |

Pending on CI (authoritative): full `mvn verify` (60+ tests), live e2e
scenarios incl. fallback drill, Docker Compose build & smoke.

## Cost

$0 — no paid APIs, no cloud resources, no LLM usage. The ranking provider
is deliberately deterministic and local.
