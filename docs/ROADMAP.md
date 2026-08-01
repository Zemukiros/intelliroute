# IntelliRoute Roadmap

## Milestone 1 — Foundation & vertical slice ✅ (current)

- [x] Monorepo structure (`apps/web`, `services/api`, `services/ai-service`, `docs`, `infrastructure`)
- [x] Graph domain model (immutable, builder-validated) and Dijkstra engine with early exit
- [x] `POST /api/routes/calculate` with validation, structured errors, measured execution time
- [x] `GET /api/routes/network` for visualization
- [x] JUnit suite: engine unit tests + full-stack MockMvc API tests
- [x] Next.js frontend: selectors, calculation, results, SVG graph with route highlighting
- [x] FastAPI ai-service scaffold with mock provider (zero-cost)
- [x] Dockerfiles + docker-compose, GitHub Actions CI with live smoke test
- [x] Documentation set

## Milestone 2 — Route alternatives & AI preferences

- [ ] K-shortest-paths (Yen's algorithm) to produce candidate route sets
- [ ] Edge metadata: road type (highway/local/scenic), toll flags, safety weighting
- [ ] Java API ↔ ai-service integration (`/rank` call with candidates)
- [ ] Natural-language preference input in the frontend
- [ ] Route comparison UI (side-by-side candidates with scores and rationale)
- [ ] Benchmark suite comparing algorithm variants on larger generated networks
- [ ] Decision + approval gate: real LLM provider (cost) vs. rule-based ranking (free)

## Milestone 3 — Persistence & realism

- [ ] PostgreSQL network storage (Spring Data JPA), replacing the in-memory bean
- [ ] Network import tooling (larger datasets, e.g. generated or open-data road graphs)
- [ ] A* with admissible heuristics; performance comparison vs. Dijkstra
- [ ] API pagination/caching where warranted; k6 or Gatling load test with recorded results

## Milestone 4 — Cloud deployment & portfolio integration

- [ ] Containerized deployment (requires cost approval: free-tier targets first)
- [ ] Frontend deployment (Vercel free tier)
- [ ] CloudWatch/metrics story consistent with AWS SAA positioning
- [ ] Architecture diagrams and portfolio case-study page
- [ ] Interactive demo embedded in the portfolio

Résumé claims are only written after the underlying work is implemented and verified — tracked in [RESUME_EVIDENCE.md](RESUME_EVIDENCE.md).
