"""Route-ranking provider interface and the cost-free mock implementation.

The provider abstraction keeps the AI service swappable: during development
(and in CI) the deterministic ``MockRankingProvider`` is used, so no paid
AI API is ever called. A future ``LlmRankingProvider`` will implement the
same protocol and be selected with the ``RANKING_PROVIDER`` env variable.
"""

from __future__ import annotations

from typing import Protocol

from pydantic import BaseModel, Field


class CandidateRoute(BaseModel):
    """A route produced by the Java routing engine."""

    path: list[str] = Field(min_length=1)
    total_distance_km: float = Field(gt=0)


class RankedRoute(BaseModel):
    """A candidate route with a preference score and explanation."""

    path: list[str]
    total_distance_km: float
    score: float
    rationale: str


class RouteRankingProvider(Protocol):
    """Ranks candidate routes against a natural-language preference."""

    def rank(
        self, preference: str, candidates: list[CandidateRoute]
    ) -> list[RankedRoute]: ...


class MockRankingProvider:
    """Deterministic, dependency-free ranking used until a real AI provider
    is integrated.

    Strategy: shorter routes score higher; the stated preference is echoed
    in the rationale so the full request/response contract is exercised
    end to end without external calls.
    """

    def rank(
        self, preference: str, candidates: list[CandidateRoute]
    ) -> list[RankedRoute]:
        if not candidates:
            return []
        longest = max(c.total_distance_km for c in candidates)
        ranked = [
            RankedRoute(
                path=c.path,
                total_distance_km=c.total_distance_km,
                score=round(1.0 - (c.total_distance_km / (longest * 1.25)), 4),
                rationale=(
                    f"Mock provider: ranked by distance pending AI integration "
                    f"(preference noted: '{preference}')."
                ),
            )
            for c in candidates
        ]
        return sorted(ranked, key=lambda r: r.score, reverse=True)
