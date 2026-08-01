# Technical Decisions

Record of significant choices and their rationale. Newest first.

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
