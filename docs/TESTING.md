# Testing

Three independent suites — Java, Python, and frontend — plus CI-level live
end-to-end tests. All suites are deterministic.

## Java (`services/api`, JUnit 5 + AssertJ + MockMvc + Mockito)

```bash
cd services/api && mvn test
```

| Suite | Covers |
|---|---|
| `DijkstraPathFinderTest` | Correct shortest paths, competing branches, reverse direction, same origin/destination, unreachable both directions, unknown nodes, visited-node bounds, minimal graphs |
| `DijkstraExclusionsTest` | Closed-road exclusion (even when shortest), edge/node exclusions, exclusion-induced unreachability |
| `YenKShortestPathsTest` | Three known A→F alternatives in order, shortest-first guarantee, uniqueness, looplessness, determinism, closed roads in candidates, fewer-than-K handling, unreachable, k=1, invalid input, metadata aggregation (distance/time/toll/safety/scenic/highway%) |
| `GraphTest` | Builder invariants, metadata mirroring, travel-time derivation, invalid values, missing lookups |
| `RankingClientTest` | Mocked HTTP integration: camelCase contract, server errors, empty results, malformed JSON → `RankingServiceException` |
| `RoutingServiceFallbackTest` | Real client against a dead port: fallback ranking, determinism, health probe |
| `RouteControllerIntegrationTest` | Full-stack MockMvc: calculate/alternatives/recommend success paths, ranking-service success (mocked) and failure→fallback, validation errors, unknown nodes, unreachable destinations, maximumRoutes bounds, network endpoint with metadata and the closed edge |

## Python (`services/ai-service`, pytest — 54 tests)

```bash
cd services/ai-service
pip install -r requirements-dev.txt
python -m pytest tests/ -q
```

| Suite | Covers |
|---|---|
| `test_parsing.py` | Individual preferences, synonym families, combined preferences, balance phrasing, repetition emphasis, unknown/ambiguous input fallback, unrecognized-term reporting, weight normalization, determinism, case/punctuation insensitivity |
| `test_ranking.py` | Expected winner per preference on the sample candidates, rank mechanics, deterministic ordering, tie-breaking by route id, indifferent criteria, empty candidate list, no-weights fallback, score bounds, explanation content, invalid route data rejection |
| `test_api.py` | HTTP contract (camelCase), health, full rank flow, unknown preference behaviour, empty candidates, validation rejections |

## Frontend (`apps/web`, Vitest + Testing Library — 9 tests)

```bash
cd apps/web && npm test
```

Covers: preference submission (request body verified), ranked-card
rendering with Recommended/Shortest badges, parsed-preference summary,
route selection updating the graph highlight, closed-road dashed
rendering, blank-preference validation, ranking-service-offline fallback
notice, structured API error rendering, example-chip behaviour, and the
API-offline recovery card.

## CI-level live end-to-end (GitHub Actions)

The backend job boots the real jar **with the real Python ranking service**
and asserts live: shortest route contract, 3 alternatives, "avoid tolls" →
toll-free route recommended, "safest" → highway route recommended,
unreachable → 422, measured end-to-end latency, and — after killing the
ranking service — automatic fallback engagement. The docker job builds the
full Compose stack and smoke-tests it. See `.github/workflows/ci.yml`.
