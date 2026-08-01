"""Route-ranking providers.

The provider abstraction keeps ranking swappable. Step 2 ships the
**local deterministic preference-ranking provider** — keyword-based
preference parsing plus normalized multi-criteria scoring. It is not an
LLM and is never described as one. A future LLM-backed or local-Ollama
provider can implement the same protocol behind the ``RANKING_PROVIDER``
environment variable without changing the API contract.
"""

from __future__ import annotations

from typing import Protocol

from .models import CandidateRoute, ParsedPreferences, RankedRoute
from .parsing import parse_preference
from .ranking import rank_routes


class RankingOutcome:
    """Provider result: interpretation + ordering."""

    def __init__(self, parsed: ParsedPreferences, results: list[RankedRoute]):
        self.parsed = parsed
        self.results = results


class RouteRankingProvider(Protocol):
    """Ranks candidate routes against a natural-language preference."""

    provider_id: str

    def rank(self, preference: str,
             candidates: list[CandidateRoute]) -> RankingOutcome: ...


class DeterministicPreferenceRankingProvider:
    """Local deterministic preference-ranking provider.

    Fully offline and reproducible: synonym/phrase parsing produces
    normalized criterion weights; routes are scored by weighted
    min-max-normalized utilities; ties break on route id.
    """

    provider_id = "local-deterministic-v1"

    def rank(self, preference: str,
             candidates: list[CandidateRoute]) -> RankingOutcome:
        outcome = parse_preference(preference)
        parsed = ParsedPreferences(
            weights=outcome.weights,
            recognized_terms=outcome.recognized_terms,
            unrecognized_terms=outcome.unrecognized_terms,
            confidence=outcome.confidence,
            note=outcome.note,
        )
        results = rank_routes(candidates, outcome.weights)
        return RankingOutcome(parsed, results)
