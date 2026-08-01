# Technical Decisions

Record of significant choices and their rationale. Newest first.

## TD-011 · Yen's algorithm for route alternatives (Step 2)

**Context:** The platform needs multiple *distinct, loopless* candidate routes, not just the single optimum.
**Decision:** Implement Yen's K-shortest loopless paths on top of the existing Dijkstra engine, extended with node/edge exclusion support (spur searches). Deterministic ordering: total distance, ties broken by lexicographic path comparison.
**Alternatives considered:** k-penalty methods (approximate, can produce loops), Eppstein's algorithm (allows loops — wrong for driving routes), plateau/via-node methods (heavier, better suited to continental-scale graphs).
**Consequence:** Exact top-K results reusing tested Dijkstra machinery; O(K·V·(E+V log V)) worst case is comfortably fast at demo scale (measured: ~7.7 ms avg for K=3 on 1,000 nodes).

## TD-010 · Deterministic keyword ranking now, LLM later — behind one interface

**Context:** Natural-language preferences are the product's signature feature, but Step 2 must stay at $0 and fully verifiable.
**Decision:** Ship a **local deterministic preference-ranking provider** (`local-deterministic-v1`): phrase/synonym parsing → normalized weights → min-max utility scoring → templated explanations. Explicitly *not* an LLM and never described as one; confidence and unrecognized terms are reported honestly. The `RouteRankingProvider` protocol and `RANKING_PROVIDER` env var keep an LLM/Ollama provider as a drop-in future addition gated on a cost decision.
**Consequence:** 100% reproducible ranking (54 tests assert exact outcomes), ~91 µs per call, zero cost, and an honest story about what the system does.

## TD-009 · Recommendation must survive a dead ranking service

**Decision:** Every transport/contract failure in `RankingClient` maps to one exception type; `RoutingService` catches it and applies a local distance-ordered fallback labelled `java-local-fallback` with `fallbackUsed: true`, surfaced in the UI as a notice. Short timeouts (1 s connect / 2 s read) keep degraded requests fast. A health indicator exposes ranking mode under `/actuator/health`.
**Rationale:** A recommendation feature that 500s when a sidecar is down is worse than no feature; graceful degradation is also demonstrated live in CI by killing the service mid-run.

## TD-008 · Deterministic, hand-authored road metadata in the sample network

**Decision:** Road types carry documented typical speeds from which travel times derive; tolls, safety, and scenic scores are hand-assigned so the A→F pair has three genuinely different winners (shortest 14 km with a toll, fastest/safest 17 km highway with 5.50 in tolls, toll-free scenic 18 km). One road (B–E) is closed to exercise closure handling. Nothing is randomized at startup; random networks exist only inside the benchmark tooling (seeded).
**Rationale:** Deterministic data makes every test assertion exact and every demo repeatable — and forces the ranking layer to make visible, explainable tradeoffs.

## TD-007 · CI as the authoritative backend verification environment

**Context:** The cloud development sandbox used to build milestone 1 blocks Maven Central at the network-policy level (npm, PyPI, and GitHub are allowed), so `mvn test` cannot download dependencies there.
**Decision:** Treat GitHub Actions as the authoritative build/test environment for the Java service. The pipeline runs `mvn -B verify` plus a live boot-and-curl smoke test on every push. Locally in the sandbox, the framework-free algorithm core (domain + engine) was additionally compiled and verified with a JDK-only harness (8 assertions + micro-benchmark), which is possible because the engine deliberately has no Spring dependencies.
**Consequence:** Backend correctness is still verified automatically and reproducibly; any contributor's machine with normal network access can run the same suite with `mvn test`.

## TD-006 · Mock AI provider behind a protocol, not a real LLM call

**Context:** AI-assisted re-ranking is a core product goal, but milestone 1 must incur zero cost.
**Decision:** Define `RouteRankingProvider` (Python `Protocol`) and ship only `MockRankingProvider` — deterministic, distance-based, echoes the preference. Provider selection via `RANKING_PROVIDER` env var.
**Consequence:** The service boundary, request/response contract, and tests exist now; swapping in an LLM provider later is additive and requires an explicit cost-approval gate.

## TD-005 · Plain SVG for graph visualization

**Context:** The frontend needs a network visualization; map SDKs (Google/Mapbox) require API keys and can bill, and chart libraries add weight without fitting a graph topology.
**Decision:** Render nodes/edges as inline SVG from abstract layout coordinates served by the API.
**Consequence:** Zero cost, zero keys, full styling control, trivially testable; geographic realism is deferred until a real dataset exists.

## TD-004 · Structured error contract via @RestControllerAdvice

**Decision:** Every failure returns one JSON problem shape (`timestamp`, `status`, `error` code, `message`, optional `details`), with distinct codes: `UNKNOWN_NODE` (404), `ROUTE_UNREACHABLE` (422), `VALIDATION_ERROR`/`MALFORMED_REQUEST` (400).
**Rationale:** 422 for "valid nodes, no path" is semantically distinct from 404 ("node doesn't exist") — the request was well-formed and understood, but unsatisfiable. The frontend renders these differently.

## TD-003 · Immutable Graph with builder validation; engine kept framework-free

**Decision:** `Graph` is deeply immutable after construction; all invariants (unique ids, known endpoints, positive finite weights, no self-loops) enforced in the builder/records. `DijkstraPathFinder` is stateless and depends only on the JDK.
**Rationale:** A shared network bean serving concurrent requests must be thread-safe; immutability achieves that without locking. A framework-free engine is independently benchmarkable and reusable (and proved valuable — see TD-007).

## TD-002 · Dijkstra with binary heap, lazy deletion, early exit

**Decision:** `PriorityQueue` + lazy deletion (skip settled entries on poll) + terminate when the destination is settled. Report settled-node count in responses.
**Rationale:** O((V+E) log V) is optimal-in-practice for sparse road networks at this scale without the complexity of a decrease-key structure; early exit measurably reduces work for nearby destinations; exposing `visitedNodes` makes algorithmic behaviour observable in the UI — a deliberate teaching/demo feature of the platform.

## TD-001 · Monorepo with independently deployable services

**Decision:** One repository (`intelliroute`) containing `apps/web`, `services/api`, `services/ai-service`, shared `docs/`, `infrastructure/`, and a single CI pipeline.
**Rationale:** Cross-service API contracts, documentation, and CI stay atomic while each service keeps its own build (Maven / npm / pip) and Dockerfile, preserving independent deployability. Polyglot boundaries (Java engine, Python AI, TypeScript UI) are a deliberate demonstration of realistic service-oriented design.
