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
| 9 | AI-assisted natural-language route preferences | **Planned** | Milestone 2 — must not appear on résumé |
| 10 | PostgreSQL-backed network persistence | **Planned** | Milestone 3 |
| 11 | AWS/cloud deployment | **Planned** | Milestone 4 |

## Measurement log

- **2026-08-01** — First CI run on GitHub ([#30706883118](https://github.com/Zemukiros/intelliroute/actions/runs/30706883118)): all 3 jobs green. `mvn verify` full suite passed; live smoke test confirmed `POST /api/routes/calculate` A→F = 14.0 km (200) and A→J unreachable (422) against the booted jar.

- **2026-08-01** — Dijkstra micro-benchmark (sandbox, JDK 21, single thread): 20,000 A→F searches after 5,000-run warmup → **4.92 µs average** per full search on the 10-node sample network. Command preserved in project history; regenerate with the CI benchmark job planned for milestone 2.
- **2026-08-01** — Frontend production build: compiled successfully; route `/` first-load JS ≈ 105 kB.
- **2026-08-01** — ESLint (`next/core-web-vitals` + `next/typescript`): 0 errors, 0 warnings.
