# IntelliRoute Roadmap

## Milestone 1 — Foundation & vertical slice ✅ (tag `v0.1.0-step1`)

- [x] Monorepo structure (`apps/web`, `services/api`, `services/ai-service`, `docs`, `infrastructure`)
- [x] Graph domain model (immutable, builder-validated) and Dijkstra engine with early exit
- [x] `POST /api/routes/calculate` with validation, structured errors, measured execution time
- [x] `GET /api/routes/network` for visualization
- [x] JUnit suite: engine unit tests + full-stack MockMvc API tests
- [x] Next.js frontend: selectors, calculation, results, SVG graph with route highlighting
- [x] FastAPI service scaffold (zero-cost)
- [x] Dockerfiles + docker-compose, GitHub Actions CI with live smoke test
- [x] Documentation set
- Verified by CI run #30706883118 and a full local browser run.

## Milestone 2 — Route alternatives & AI preferences ✅ (Step 2, this branch)

- [x] Road metadata: types with typical speeds, travel time, tolls, safety, scenic, open/closed
- [x] Yen's K-shortest loopless paths (deterministic, closed-road aware) + `POST /api/routes/alternatives`
- [x] Local deterministic preference-ranking provider (parsing, synonyms, combined preferences, weights, confidence, explanations, safe fallbacks)
- [x] Java ↔ ranking-service integration with timeouts, health check, and graceful local fallback + `POST /api/routes/recommend`
- [x] Natural-language preference input and side-by-side route comparison UI with recommended/shortest distinction
- [x] Test suites across all three services; CI live e2e incl. fallback drill; Docker Compose CI job
- [x] Reproducible benchmarks on 100/1k/5k-node generated networks with recorded methodology
- [ ] Optional (deferred, cost-gated): real LLM / local-Ollama ranking provider behind the existing interface

## Milestone 3 — Persistence & realism

- [ ] PostgreSQL network storage (Spring Data JPA), replacing the in-memory bean
- [ ] Network import tooling (larger datasets, e.g. generated or open-data road graphs)
- [ ] A* with admissible heuristics; performance comparison vs. Dijkstra
- [ ] Authentication for network-management endpoints
- [ ] API pagination/caching where warranted; load test with recorded results

## Milestone 4 — Cloud deployment & portfolio integration

- [ ] Containerized deployment (requires cost approval: free-tier targets first)
- [ ] Frontend deployment (Vercel free tier)
- [ ] CloudWatch/metrics story consistent with AWS SAA positioning
- [ ] Architecture diagrams and portfolio case-study page
- [ ] Interactive demo embedded in the portfolio

Résumé claims are only written after the underlying work is implemented and verified — tracked in [RESUME_EVIDENCE.md](RESUME_EVIDENCE.md).
