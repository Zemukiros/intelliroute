# Changelog

All notable changes to IntelliRoute. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions are milestone
tags.

## [Unreleased] — 0.2.0 (Step 2: Multi-Route Intelligence)

### Added
- Road metadata model: road types (highway/arterial/local/scenic) with
  typical speeds, derived travel times, toll costs, safety and scenic
  scores (0–10), open/closed status; builder-validated.
- Closed-road support end to end: never routed, shown dashed in the UI
  (sample network gained the closed B–E road).
- Yen's K-shortest loopless-paths algorithm on the Dijkstra core
  (exclusion support added), with uniqueness, looplessness, deterministic
  ordering, and search statistics.
- `POST /api/routes/alternatives` — up to 5 candidate routes with
  aggregated metrics (distance, time, tolls, distance-weighted safety and
  scenic scores, highway percentage).
- Local deterministic preference-ranking provider
  (`local-deterministic-v1`) in the Python service: phrase/synonym
  parsing, combined preferences, weight normalization, min-max utility
  scoring, per-route explanations, confidence reporting, safe fallback
  for unclear input.
- `POST /api/routes/recommend` — full flow with measured routing and
  ranking times, parsed preferences, ranked routes with explanations, and
  graceful `java-local-fallback` when the ranking service is offline
  (plus a ranking-service health indicator under `/actuator/health`).
- Frontend route-intelligence UI: natural-language preference input with
  example chips, maximum-routes selector, ranked comparison cards with
  Recommended/Shortest badges and metric grids, route selection with graph
  highlighting, offline/fallback/validation/empty/loading states,
  keyboard-accessible cards.
- Test suites: 24 → 60+ Java tests, 54 Python tests, 9 frontend
  component tests (Vitest + Testing Library).
- Reproducible benchmark utilities (Java generated networks at
  100/1k/5k nodes; Python ranking benchmark) with recorded results in
  docs/BENCHMARKING.md.
- CI: live e2e job (real jar + real ranking service + fallback drill),
  Python test/benchmark job, frontend test step, Docker Compose
  build-and-smoke job.
- Docs: ROUTE_RANKING.md, TESTING.md, BENCHMARKING.md,
  STEP2_DEVELOPMENT_LOG.md, CHANGELOG.md.

### Changed
- `GET /api/routes/network` now includes road metadata and closed status.
- Docker Compose: api now depends on ai-service with correct relaxed-
  binding env names and a dependency-free healthcheck.

### Preserved
- `POST /api/routes/calculate` contract and the A→F shortest route
  (A-C-D-F, 14 km) are unchanged from Step 1.

## [0.1.0] — 2026-08-01 (Step 1, tag `v0.1.0-step1`)

Initial release: monorepo foundation; Java 21/Spring Boot 3.4 routing API
with immutable graph domain and Dijkstra engine (binary heap, lazy
deletion, early exit); measured execution times; structured error
contract; 10-town sample network with a disconnected island; Next.js 15
frontend with SVG network visualization; FastAPI service scaffold with
mock provider; Dockerfiles + Compose; GitHub Actions CI with live smoke
test; full documentation set. Verified by CI run #30706883118.
