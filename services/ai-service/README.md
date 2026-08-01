# IntelliRoute AI Service (scaffold)

FastAPI service that will re-rank candidate routes against natural-language
preferences ("avoid highways", "prefer the safest route", …).

**Status: architectural scaffold.** In Step 1 the service exposes a health
endpoint and a documented mock re-ranking provider so the platform's service
boundary exists without incurring any AI-API costs. No paid providers are
called anywhere in this codebase.

## Design

- `app/main.py` — FastAPI application and routes
- `app/providers.py` — `RouteRankingProvider` protocol plus `MockRankingProvider`.
  A real LLM-backed provider will implement the same protocol in a later
  milestone, selected via the `RANKING_PROVIDER` environment variable.

## Run locally

```bash
cd services/ai-service
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 8090 --reload
```

Then: `curl http://localhost:8090/health`
