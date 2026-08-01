# IntelliRoute

**AI-assisted route intelligence platform** — shortest-path routing over weighted road networks with measured performance, built as a cloud-ready microservice architecture.

> Milestone 1 (current): working vertical slice — Java/Spring Boot routing engine with Dijkstra's algorithm, REST API, interactive Next.js frontend with graph visualization, and a cost-free AI-service scaffold. Natural-language route preferences ("avoid highways", "prefer the safest route") arrive in a later milestone; the service boundary for them already exists.

## Architecture at a glance

```
┌──────────────┐   REST/JSON    ┌───────────────────┐   internal REST   ┌────────────────┐
│  apps/web    │ ─────────────► │  services/api      │ ────────────────► │ services/      │
│  Next.js 15  │                │  Spring Boot 3     │   (Milestone 3)   │ ai-service     │
│  TypeScript  │ ◄───────────── │  Java 21 + Maven   │ ◄──────────────── │ FastAPI (mock) │
│  Tailwind    │                │  Dijkstra engine   │                   │ provider)      │
└──────────────┘                └───────────────────┘                   └────────────────┘
```

- **services/api** — Spring Boot routing engine: immutable graph domain model, Dijkstra with early exit and lazy deletion (O((V+E) log V)), validated REST endpoints, structured error contract, measured execution time.
- **apps/web** — Next.js 15 + TypeScript + Tailwind: origin/destination selection, live route calculation, SVG network visualization with route highlighting.
- **services/ai-service** — FastAPI scaffold with a swappable `RouteRankingProvider` interface; ships a deterministic mock provider so no paid AI API is ever called during development.

Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · [docs/API.md](docs/API.md) · [docs/ROADMAP.md](docs/ROADMAP.md) · [docs/TECHNICAL_DECISIONS.md](docs/TECHNICAL_DECISIONS.md)

## Quick start

Prerequisites: Java 21, Maven 3.9+, Node 20+, (optional) Python 3.11+.

**1. Start the API** (port 8080):

```bash
cd services/api
mvn spring-boot:run
```

**2. Start the frontend** (port 3000):

```bash
cd apps/web
npm install
npm run dev
```

Open http://localhost:3000, pick an origin and destination, and calculate a route.

**3. (Optional) AI-service scaffold** (port 8090):

```bash
cd services/ai-service
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 8090
```

Or run everything with Docker: `docker compose up --build`

## Try the API directly

```bash
curl -X POST http://localhost:8080/api/routes/calculate \
  -H "Content-Type: application/json" \
  -d '{"origin": "A", "destination": "F"}'
```

```json
{
  "origin": "A",
  "destination": "F",
  "path": ["A", "C", "D", "F"],
  "totalDistance": 14.0,
  "visitedNodes": 6,
  "executionTimeMs": 0.012
}
```

`executionTimeMs` is measured per request with `System.nanoTime()` — never hard-coded.

## Testing

```bash
cd services/api && mvn test        # engine unit tests + full-stack MockMvc API tests
cd apps/web && npm run lint && npm run build
```

CI runs the same suites on every push, plus a live boot-and-curl smoke test — see [.github/workflows/ci.yml](.github/workflows/ci.yml).

## Project status

This is milestone 1 of a four-milestone roadmap (AI preference re-ranking, PostgreSQL-backed networks, authentication, AWS deployment). See [docs/ROADMAP.md](docs/ROADMAP.md).

## License

MIT
