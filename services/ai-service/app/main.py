"""IntelliRoute ranking service — FastAPI application.

Exposes the local deterministic preference-ranking provider. No paid AI
APIs are called anywhere in this service.
"""

from __future__ import annotations

import os

from fastapi import FastAPI

from .models import RankRequest, RankResponse
from .providers import DeterministicPreferenceRankingProvider, RouteRankingProvider

app = FastAPI(
    title="IntelliRoute Ranking Service",
    version="0.2.0",
    description="Ranks candidate routes against natural-language preferences"
                " using a local deterministic provider.",
)


def _select_provider() -> RouteRankingProvider:
    provider_name = os.getenv("RANKING_PROVIDER", "deterministic")
    if provider_name in {"deterministic", "mock"}:
        # "mock" retained as an alias for backwards compatibility with Step 1.
        return DeterministicPreferenceRankingProvider()
    raise RuntimeError(f"Unknown RANKING_PROVIDER: {provider_name}")


provider = _select_provider()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "provider": provider.provider_id}


@app.post("/rank", response_model=RankResponse)
def rank(request: RankRequest) -> RankResponse:
    outcome = provider.rank(request.preference, request.candidates)
    return RankResponse(
        provider=provider.provider_id,
        parsed_preferences=outcome.parsed,
        results=outcome.results,
        recommended_route_id=(outcome.results[0].route_id
                              if outcome.results else None),
    )
