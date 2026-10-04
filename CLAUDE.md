# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

IntelliRoute is a three-service monorepo: a Java routing engine, a Python preference-ranking
service, and a Next.js frontend. Milestone 2 of four is complete (multi-route generation +
preference ranking); milestone 3 is PostgreSQL persistence and A*. See `docs/ROADMAP.md`.
The current build is already deployed on free tiers (web on Vercel, api + ai-service on Render)
ahead of the rest of milestone 4 — see `DEPLOYMENT.md`.

## Commands

Each service builds independently. Ports: web 3000, api 8080, ai-service 8090.

```bash
# services/api (Java 21, Maven)
mvn spring-boot:run
mvn test
mvn test -Dtest=YenKShortestPathsTest              # single class
mvn test -Dtest=YenKShortestPathsTest#threeAlternativesForAtoF   # single method
mvn -B verify                                       # what CI runs
mvn -q compile exec:java -Dexec.mainClass=com.intelliroute.api.bench.BenchmarkRunner

# services/ai-service (Python 3.12, FastAPI)
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt                 # requirements.txt = runtime only
uvicorn app.main:app --port 8090
python -m pytest tests/ -q
python -m pytest tests/test_parsing.py -k synonym   # single test
python scripts/benchmark_ranking.py

# apps/web (Node 22, Next 15, React 19)
npm install && npm run dev
npm test                                            # vitest run
npx vitest run src/components/RoutePlanner.test.tsx -t "fallback"
npm run lint && npm run build

# whole stack
docker compose up --build
```

The API runs fine without the ranking service — it falls back and labels the response. The
frontend runs against a live API only; there is no mock server.

## Architecture

Request flow: `apps/web` → `POST /api/routes/recommend` (Spring) → `POST /rank` (FastAPI).

**services/api** — strictly layered, dependencies inward:
`web` (controllers/DTOs/`@RestControllerAdvice`) → `service` (`RoutingService`) → `engine`
(`DijkstraPathFinder`, `YenKShortestPaths`, `RouteCandidate`) → `domain` (`Graph`, `Edge`, `Node`).

- `domain` and `engine` are **framework-free** (JDK only) and must stay that way — they are
  benchmarked and compiled standalone with plain `javac` when Maven Central is unreachable
  (see TD-007). Don't introduce Spring annotations or imports below the `service` layer.
- `Graph` is deeply immutable and builder-validated; the shared network bean is therefore
  thread-safe with no locking. Two-way roads are two mirrored directed edges.
- `YenKShortestPaths` drives spur searches through Dijkstra's node/edge **exclusion** support —
  that exclusion API exists for Yen, so changes to `DijkstraPathFinder` must preserve it.
- The road network comes from `SampleNetworkConfig` (10 nodes, 16 two-way roads, B–E closed,
  "J" intentionally disconnected). Milestone 3 replaces this bean with a repository; keep the
  engine and web layers ignorant of where the graph came from.
- `RankingClient` maps *every* transport/contract failure to `RankingServiceException`;
  `RoutingService.recommend` catches it and produces a distance-ordered fallback labelled
  `java-local-fallback` with `fallbackUsed: true`. The recommend endpoint must never 5xx
  because the Python service is down — CI proves this by killing the ranker mid-run.
- All timings in responses are measured with `System.nanoTime()` around real work. Never
  estimate or hard-code a timing value.

**services/ai-service** — the ranking provider, *not an LLM*. Provider id
`local-deterministic-v1`: `parsing.py` (phrase-first matching, then keyword/synonyms →
normalized weights + confidence + unrecognized terms) → `ranking.py` (min-max normalized
multi-criteria utility, deterministic tie-break by route id, templated explanations).
`providers.py` defines the `RouteRankingProvider` protocol selected by the `RANKING_PROVIDER`
env var — an LLM/Ollama provider is a future drop-in there, deliberately deferred and
cost-gated. Never describe this service as AI/LLM-powered in code, docs, or UI copy.

**apps/web** — App Router; all interactivity in the `RoutePlanner` client component.
`src/lib/api.ts` is the only module that talks to the backend and converts problem responses
into typed `ApiError`s. `GraphView` renders the network as inline SVG (no map SDK, no keys).
Degradation states (API offline, ranking offline, validation error, empty) are first-class
product surfaces with tests — keep them when changing the UI.

## Contracts to keep in sync

Changing any route/ranking field means touching all three sides at once:

- Wire format is **camelCase everywhere**. Python uses `CamelModel` in `app/models.py`
  (`alias_generator=to_camel`) — add new fields to that base, never raw `BaseModel`.
- Java DTOs live in `web/dto` (client-facing) and `ranking/RankingDtos` (the `/rank` contract);
  TypeScript mirrors in `apps/web/src/lib/types.ts`.
- Error shape is one JSON problem body for all failures: `UNKNOWN_NODE` 404,
  `ROUTE_UNREACHABLE` 422 (valid nodes, no path), `VALIDATION_ERROR`/`MALFORMED_REQUEST` 400.
- CI asserts exact live values against the sample network — e.g. `A→F` shortest is
  `totalDistance: 14.0`, "avoid tolls" and "avoid highways" must recommend `route-3`,
  "safest" must recommend `route-2`, `A→J` must be 422. Editing `SampleNetworkConfig` or the
  ranking weights breaks `.github/workflows/ci.yml` and the Java/Python suites together.

## Conventions

- Everything is deterministic: no randomness at runtime, seeded (42) only inside benchmark
  tooling. Tests assert exact winners and orderings, so ranking changes are contract changes.
- Configuration is externalized under `intelliroute.*` in `application.yml` and overridden in
  Docker/Render via relaxed-binding env vars (`INTELLIROUTE_RANKING_BASEURL`,
  `INTELLIROUTE_CORS_ALLOWEDORIGINS`). The frontend's only variable is `NEXT_PUBLIC_API_BASE_URL`.
- Record significant choices as a new newest-first `TD-0NN` entry in `docs/TECHNICAL_DECISIONS.md`,
  and keep `CHANGELOG.md` / `docs/ROADMAP.md` current. `docs/RESUME_EVIDENCE.md` follows the rule
  that a claim is only written after the work is implemented and verified — don't overstate
  capability anywhere in the docs.
- No cloud resources are provisioned from this repo; deployment (Render blueprint in
  `render.yaml`, Vercel for the frontend) is documented in `DEPLOYMENT.md` and free-tier only.

## Workflow
Plans and research live in docs/plans/. When asked to implement a plan,
read the relevant file in docs/plans/ first and follow it. Ask before
deviating from a plan in a major way. After finishing a significant
feature, update this file if the project structure changed.
