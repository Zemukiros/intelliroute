# IntelliRoute Architecture

## System overview

IntelliRoute is a small microservice platform with three components in a monorepo:

| Component | Path | Stack | Responsibility |
|---|---|---|---|
| Web frontend | `apps/web` | Next.js 15, TypeScript, Tailwind CSS | Interactive route planning UI and network visualization |
| Routing API | `services/api` | Java 21, Spring Boot 3.4, Maven | Graph domain model, Dijkstra engine, REST API |
| AI service | `services/ai-service` | Python 3.12, FastAPI | (Scaffold) natural-language route re-ranking behind a provider interface |

The monorepo keeps cross-service contracts, docs, and CI in one place while each service stays independently buildable and deployable.

## Routing API (services/api)

Layered architecture, dependencies pointing inward:

```
web (controllers, DTOs, error mapping)
  └── service (RoutingService: orchestration, timing, validation)
        └── engine (DijkstraPathFinder, PathResult — pure algorithms)
              └── domain (Graph, Node, Edge — immutable model)
```

- **domain** — `Graph` is immutable and built through a builder that validates node uniqueness, edge endpoints, and positive finite weights. Two-way roads are stored as two directed edges. Immutability makes the shared network bean thread-safe with no locking.
- **engine** — `DijkstraPathFinder` is a stateless, Spring-free class (the `@Component` annotation is its only framework touchpoint). Binary-heap priority queue with lazy deletion; early exit when the destination is settled. Complexity O((V+E) log V), space O(V). It reports `visitedNodes` (settled count) as a transparent measure of search effort.
- **service** — `RoutingService` validates node existence (translating to typed exceptions), times the engine call with `System.nanoTime()`, and maps unreachable results to a 422 error.
- **web** — thin controllers; a `@RestControllerAdvice` maps every failure mode to one consistent JSON problem shape (see [API.md](API.md)).

The sample network is provided by `SampleNetworkConfig` as a Spring bean: 10 towns, 15 two-way roads, and one intentionally disconnected island node ("J") so unreachable-route behaviour is demonstrable end to end. In milestone 3 this bean is replaced by a PostgreSQL-backed `NetworkRepository` without touching the engine or web layers.

## Frontend (apps/web)

- App Router with a single server-rendered page shell; interactivity lives in the `RoutePlanner` client component.
- `src/lib/api.ts` is the only module that talks to the backend; it surfaces the API's structured problem responses as typed `ApiError`s.
- `GraphView` renders the network as plain SVG from layout coordinates served by `GET /api/routes/network` — no map SDK, no chart library, no API keys.
- Offline handling: if the API is unreachable the UI shows a recovery card with the exact command to start the backend, rather than a broken page.

## AI service (services/ai-service)

Exists in milestone 1 so the service boundary and contract are real before any AI cost is incurred:

- `RouteRankingProvider` protocol: `rank(preference, candidates) -> ranked routes with scores and rationale`.
- `MockRankingProvider`: deterministic distance-based ranking, echoes the preference in its rationale. Selected via `RANKING_PROVIDER=mock` (default and only option in milestone 1).
- A future LLM-backed provider implements the same protocol; the Java API will call `/rank` with candidate routes produced by k-shortest-path search (milestone 2).

## Cross-cutting decisions

- **CORS** — externalized via `intelliroute.cors.allowed-origins`; defaults to `http://localhost:3000` for development.
- **Configuration** — no secrets exist in milestone 1; `.env.example` documents the only frontend variable (`NEXT_PUBLIC_API_BASE_URL`).
- **Errors** — one problem JSON shape for all API failures: `UNKNOWN_NODE` (404), `ROUTE_UNREACHABLE` (422), `VALIDATION_ERROR` / `MALFORMED_REQUEST` (400).
- **Docker** — each service ships a Dockerfile; `docker-compose.yml` wires the local stack with health-checked startup ordering.

## Cloud readiness (planned, not yet provisioned)

The target AWS shape (milestone 4) is: frontend on Vercel free tier or S3+CloudFront; API and AI service as containers (Elastic Beanstalk or ECS on Fargate); RDS PostgreSQL for network storage; CloudWatch for metrics. Nothing cloud-side is created yet — cost control requires explicit approval first.
