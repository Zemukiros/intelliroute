"""IntelliRoute AI service — FastAPI application.

Step 1 scaffold: exposes /health and /rank backed by the mock provider.
No paid AI APIs are called.
"""

from __future__ import annotations

import os

from fastapi import FastAPI
from pydantic import BaseModel, Field

from .providers import CandidateRoute, MockRankingProvider, RankedRoute

app = FastAPI(
    title="IntelliRoute AI Service",
    version="0.1.0",
    description="Re-ranks candidate routes against natural-language preferences.",
)


def _select_provider() -> MockRankingProvider:
    provider_name = os.getenv("RANKING_PROVIDER", "mock")
    # Only the mock provider exists in Step 1; future providers register here.
    if provider_name != "mock":
        raise RuntimeError(f"Unknown RANKING_PROVIDER: {provider_name}")
    return MockRankingProvider()


provider = _select_provider()


class RankRequest(BaseModel):
    preference: str = Field(min_length=1, max_length=500)
    candidates: list[CandidateRoute]


class RankResponse(BaseModel):
    provider: str
    results: list[RankedRoute]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "provider": type(provider).__name__}


@app.post("/rank", response_model=RankResponse)
def rank(request: RankRequest) -> RankResponse:
    return RankResponse(
        provider=type(provider).__name__,
        results=provider.rank(request.preference, request.candidates),
    )
