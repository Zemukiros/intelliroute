# IntelliRoute

**AI-assisted route intelligence platform** — multi-route generation with graph algorithms, natural-language route preferences, and explainable ranking, built as a cloud-ready microservice architecture.

> Milestone 2 (current): the platform generates up to five distinct candidate routes with Yen's k-shortest loopless-paths algorithm, models real road tradeoffs (travel time, tolls, safety, scenery, highways, closures), and ranks candidates against plain-language preferences like *"choose the safest route and avoid tolls"* via a local deterministic ranking service — with per-route explanations, graceful fallback, and measured timings throughout.

## Architecture at a glance

```
┌──────────────┐   REST/JSON    ┌────────────────────┐    REST/JSON     ┌────────────────────┐
│  apps/web    │ ─────────────► │  services/api       │ ───────────────► │ services/ai-service │
│  Next.js 15  │                │  Spring Boot 3      │  /rank contract  │ FastAPI             │
│  TypeScript  │ ◄───────────── │  Java 21 + Maven    │ ◄─────────────── │ deterministic       │
│  Tailwind    │                │  Dijkstra + Yen     │  (+ local        │ preference ranking  │
└──────────────┘                │  route metrics      │   fallback)      └────────────────────┘
                                └────────────────────┘
```

- **services/api** — immutable graph domain with road metadata (types, tolls, safety, scenery, closures), Dijkstra (binary heap, lazy deletion, early exit, exclusions) and Yen's K-shortest loopless paths, aggregated route metrics, validated REST endpoints, structured errors, measured execution times, and a resilient ranking integration that degrades to a local fallback instead of failing.
- **services/ai-service** — the *local deterministic preference-ranking provider*: phrase/synonym parsing → normalized criterion weights → min-max utility scoring → per-route explanations, with confidence reporting and safe fallbacks. Not an LLM; a real LLM provider can drop in behind the same interface later.
- **apps/web** — natural-language preference input with examples, ranked side-by-side route comparison cards (Recommended vs Shortest), selection-driven SVG graph highlighting, and honest offline/fallback states.

Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · [docs/API.md](docs/API.md) · [docs/ROUTE_RANKING.md](docs/ROUTE_RANKING.md) · [docs/BENCHMARKING.md](docs/BENCHMARKING.md) · [docs/TESTING.md](docs/TESTING.md) · [docs/ROADMAP.md](docs/ROADMAP.md) · [docs/TECHNICAL_DECISIONS.md](docs/TECHNICAL_DECISIONS.md) · [CHANGELOG.md](CHANGELOG.md)

## Quick start

Prerequisites: Java 21, Maven 3.9+, Node 20+, Python 3.11+.

```bash
# 1. Ranking service (port 8090)
cd services/ai-service
python3 -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --port 8090
```

```bash
# 2. API (port 8080)
cd services/api
mvn spring-boot:run
```

```bash
# 3. Frontend (port 3000)
cd apps/web
npm install
npm run dev
```

Open http://localhost:3000, pick origin/destination, describe a preference (or click an example), and compare the ranked routes. The API also works without the ranking service — it falls back to distance ordering and says so.

Or run everything with Docker: `docker compose up --build`

## Try the API directly

```bash
curl -X POST http://localhost:8080/api/routes/recommend \
  -H "Content-Type: application/json" \
  -d '{"origin": "A", "destination": "F", "preference": "Choose the safest route and avoid tolls", "maximumRoutes": 3}'
```

Response (abridged): three candidates with metrics, parsed preference weights `{"safety": 0.5, "toll": 0.5}`, ranked routes with explanations, `recommendedRouteId`, and measured `routingTimeMs` / `rankingTimeMs`. All endpoints: [docs/API.md](docs/API.md).

## Testing & benchmarks

```bash
cd services/api && mvn test                      # 60+ JUnit tests (engine, Yen, client, fallback, MockMvc)
cd services/ai-service && python -m pytest       # 54 tests (parsing, ranking, HTTP contract)
cd apps/web && npm test && npm run lint && npm run build   # 9 component tests
```

CI runs all suites plus a **live end-to-end job** (real jar + real ranking service, preference scenarios, fallback drill with the service killed mid-run) and a Docker Compose build-and-smoke job — see [.github/workflows/ci.yml](.github/workflows/ci.yml).

Reproducible benchmarks (seeded generated networks at 100/1k/5k nodes, warm-up, avg/median/p95) with recorded results: [docs/BENCHMARKING.md](docs/BENCHMARKING.md).

## Project status

Milestone 2 of four (next: PostgreSQL persistence & A*; then cloud deployment). See [docs/ROADMAP.md](docs/ROADMAP.md).

## License

MIT
