# Route Ranking

How IntelliRoute turns "choose the safest route and avoid tolls" into a
ranked list of routes — and how the pieces differ.

## The four layers (and what each one is)

| Layer | What it is | Status |
|---|---|---|
| Dijkstra shortest path | Exact single-route optimization on edge distance | Implemented (Step 1) |
| Yen's K-shortest loopless paths | Exact top-K route *generation* — produces the candidate set | Implemented (Step 2) |
| Deterministic preference ranking | Keyword parsing + weighted multi-criteria scoring of the candidates | Implemented (Step 2) |
| LLM-assisted ranking | Optional future provider for free-form language understanding | Planned, behind the same interface |

**Dijkstra** answers "what is *the* shortest route?" — one optimal answer,
one metric. **Yen's algorithm** answers "what are the K best distinct
routes?" by repeatedly re-running Dijkstra with nodes/edges of previous
results removed (spur paths), guaranteeing loopless, unique, deterministic
candidates. **Preference ranking** then reorders those candidates against
what the user *cares about* — it does not find routes, it judges them.

## The local deterministic preference-ranking provider

Provider id: `local-deterministic-v1`. This is **not an LLM** — it is a
transparent, fully offline, reproducible pipeline:

### 1. Preference parsing (`app/parsing.py`)

- Normalizes text (lowercase, strip punctuation).
- Matches **phrases first** ("avoid highways", "toll-free", "minimize
  travel time") so multi-word intent is consumed as one unit, then single
  **keywords and synonyms** (fastest/quick/rapid → time; cheap/fees → toll;
  beautiful/views → scenic; …).
- Supports **combined preferences** — each mention accumulates weight, and
  weights are normalized to sum to 1.0 ("safest + avoid tolls" → safety 0.5,
  toll 0.5). Repetition adds emphasis ("fast fast fast but cheap" weights
  time 3:1 over toll).
- "Balance X, Y, Z" evens the weights across the mentioned criteria.
- Reports **recognized terms**, **unrecognized terms** (content words it
  ignored), a **confidence** value (share of content words recognized), and
  a human-readable note.
- **Safe fallback:** if nothing is recognized, it uses a documented balanced
  default (time/distance/toll/safety at 0.25 each) with confidence 0 and
  says so — unclear input never errors and never guesses silently.

Weight keys: `time`, `distance`, `toll`, `safety`, `scenic`, `avoidHighways`.

Known limitation (documented, tested): negation like "not fast" is not
understood — the parser sees "fast". Unsupported words are listed back to
the caller rather than misinterpreted.

### 2. Scoring (`app/ranking.py`)

For each active criterion, candidate values are **min-max normalized** to a
0..1 utility (lower-is-better metrics inverted; if all candidates tie the
criterion is neutral). A route's score is the weight-sum of its utilities.
Ties break deterministically by route id. Every route gets a generated
**explanation** naming the criteria that drove its rank ("lowest toll cost
(0.00 in tolls); highest scenery (8.3/10)…").

### 3. Java integration (`RankingClient` → `POST /rank`)

The Spring API sends candidates with aggregated metadata; timeouts are
configured (`intelliroute.ranking.*`); any failure — refused connection,
timeout, bad payload — triggers the **local fallback**: distance-ordered
ranking labelled `java-local-fallback` with `fallbackUsed: true`, so the
recommendation endpoint never breaks when the Python service is down.

## Swapping in a real LLM later

`RouteRankingProvider` (Python `Protocol`) is the seam. An
`LlmRankingProvider` or local-Ollama provider would implement the same
`rank(preference, candidates)` shape and be selected via the
`RANKING_PROVIDER` environment variable — no Java or frontend changes
required. This is deliberately deferred: it requires a cost decision
(hosted LLM) or a local-model install (Ollama), gated on explicit approval.
