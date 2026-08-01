# IntelliRoute Ranking Service

FastAPI service that ranks candidate routes against natural-language
preferences ("choose the safest route and avoid tolls").

**Provider: `local-deterministic-v1`** — a local deterministic
preference-ranking provider: phrase/synonym parsing → normalized criterion
weights → min-max utility scoring → per-route explanations, with
confidence reporting and safe fallbacks for unclear input. It is **not an
LLM**; it is fully offline, reproducible, and free. The
`RouteRankingProvider` protocol keeps an LLM or local-Ollama provider as a
future drop-in behind the `RANKING_PROVIDER` environment variable
(cost-gated).

Parsing and scoring rules, wire contract, and layer comparison:
[../../docs/ROUTE_RANKING.md](../../docs/ROUTE_RANKING.md)

## Layout

- `app/models.py` — pydantic models (camelCase wire format shared with the Java API)
- `app/parsing.py` — preference parsing (phrases, synonyms, combinations, fallbacks)
- `app/ranking.py` — normalized multi-criteria scoring + explanations
- `app/providers.py` — provider protocol + deterministic provider
- `app/main.py` — FastAPI app (`GET /health`, `POST /rank`)
- `tests/` — 54 pytest tests
- `scripts/benchmark_ranking.py` — reproducible benchmark (seed 42)

## Run locally

```bash
cd services/ai-service
python3 -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --port 8090
```

## Test & benchmark

```bash
pip install -r requirements-dev.txt
python -m pytest tests/ -q
python scripts/benchmark_ranking.py
```
