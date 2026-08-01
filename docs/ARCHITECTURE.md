# IntelliRoute Architecture

## System overview

IntelliRoute is a small microservice platform with three components in a monorepo:

| Component | Path | Stack | Responsibility |
|---|---|---|---|
| Web frontend | `apps/web` | Next.js 15, TypeScript, Tailwind CSS | Preference input, ranked route comparison, network visualization |
| Routing API | `services/api` | Java 21, Spring Boot 3.4, Maven | Graph domain, Dijkstra + Yen engines, recommendation orchestration, REST API |
| Ranking service | `services/ai-service` | Python 3.12, FastAPI | Local deterministic preference parsing and route ranking behind a provider interface |

The monorepo keeps cross-service contracts, docs, and CI in one place while each service stays independently buildable and deployable.

## Routing API (services/api)

Layered architecture, dependencies pointing inward:

```
web (controllers, DTOs, error mapping)
  └── service (RoutingService: orchestration, timing, validation)
        └── engine (DijkstraPathFinder, PathResult — pure algorithms)
              └── domain (Graph, Node, Edge — immutable model)
```

- **domain** — `Graph` is immutable and built through a builder that validates node uniqueness, edge endpoints, and value ranges. `Edge` carries full road metadata: distance, derived travel time, `RoadType` (with typical speeds), toll, safety/scenic scores, and open/closed status. Two-way roads are stored as two mirrored directed edges. Immutability makes the shared network bean thread-safe with no locking.
- **engine** — Spring-free algorithms. `DijkstraPathFinder`: binary-heap priority queue with lazy deletion, early exit, closed-road awareness, and node/edge exclusion support; O((V+E) log V). `YenKShortestPaths`: K shortest loopless paths via spur searches over the exclusion-capable Dijkstra — unique, loopless, deterministic, with search statistics. `RouteCandidate` aggregates per-route metrics (distance-weighted safety/scenic, highway share, tolls, time).
- **ranking** — `RankingClient` (RestClient, configurable timeouts) speaks the camelCase `/rank` contract; every failure maps to one exception so callers can degrade. A health indicator reports `remote-ranking` vs `local-fallback` mode.
- **service** — `RoutingService` orchestrates: shortest route (Step 1 contract preserved), alternatives (Yen + metrics), and recommendation (alternatives → ranking service → measured timings), with a deterministic local distance-ordered fallback labelled `java-local-fallback` when ranking is unavailable.
- **web** — thin controllers; a `@RestControllerAdvice` maps every failure mode to one consistent JSON problem shape (see [API.md](API.md)).

The sample network is provided by `SampleNetworkConfig` as a Spring bean: 10 towns, 16 two-way roads (one closed for construction), and one intentionally disconnected island node ("J"). Road metadata is deterministic and documented in the config — the A→F pair deliberately offers three routes with different tradeoffs so preference ranking is demonstrable. In milestone 3 this bean is replaced by a PostgreSQL-backed `NetworkRepository` without touching the engine or web layers.

## Frontend (apps/web)

- App Router with a single server-rendered page shell; interactivity lives in the `RoutePlanner` client component (preference textarea with example chips, max-routes selector, ranked `RouteCard` comparison list with Recommended/Shortest badges, selection-driven graph highlighting).
- `src/lib/api.ts` is the only module that talks to the backend; it surfaces the API's structured problem responses as typed `ApiError`s.
- `GraphView` renders the network as plain SVG from `GET /api/routes/network` — closed roads dashed, selected route highlighted; no map SDK, no API keys.
- Degradation states are first-class: API offline (recovery card), ranking service offline (fallback notice), validation errors, empty state. Component behaviour is covered by Vitest + Testing Library tests.

## Ranking service (services/ai-service)

The **local deterministic preference-ranking provider** (`local-deterministic-v1`):

- `app/parsing.py` — phrase/synonym parsing of natural-language preferences into normalized criterion weights with confidence and unrecognized-term reporting; safe balanced fallback for unclear input.
- `app/ranking.py` — min-max-normalized multi-criteria scoring with deterministic tie-breaking and templated per-route explanations.
- `app/providers.py` — `RouteRankingProvider` protocol; an LLM or local-Ollama provider can be added behind `RANKING_PROVIDER` later (cost-gated) without touching the Java or frontend layers.

Full rules and the wire contract: [ROUTE_RANKING.md](ROUTE_RANKING.md).

## Cross-cutting decisions

- **CORS** — externalized via `intelliroute.cors.allowed-origins`; defaults to `http://localhost:3000` for development.
- **Configuration** — no secrets exist in milestone 1; `.env.example` documents the only frontend variable (`NEXT_PUBLIC_API_BASE_URL`).
- **Errors** — one problem JSON shape for all API failures: `UNKNOWN_NODE` (404), `ROUTE_UNREACHABLE` (422), `VALIDATION_ERROR` / `MALFORMED_REQUEST` (400).
- **Docker** — each service ships a Dockerfile; `docker-compose.yml` wires the local stack with health-checked startup ordering.

## Cloud readiness (planned, not yet provisioned)

The target AWS shape (milestone 4) is: frontend on Vercel free tier or S3+CloudFront; API and AI service as containers (Elastic Beanstalk or ECS on Fargate); RDS PostgreSQL for network storage; CloudWatch for metrics. Nothing cloud-side is created yet — cost control requires explicit approval first.
