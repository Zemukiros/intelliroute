"""Pydantic models shared across the ranking service.

Wire format uses camelCase (matching the Java API's DTOs); Python code uses
snake_case internally via alias generation.
"""

from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class CandidateRoute(CamelModel):
    """A candidate route produced by the Java routing engine."""

    route_id: str = Field(min_length=1)
    path: list[str] = Field(min_length=1)
    total_distance_km: float = Field(gt=0)
    travel_time_minutes: float = Field(gt=0)
    toll_cost: float = Field(ge=0)
    safety_score: float = Field(ge=0, le=10)
    scenic_score: float = Field(ge=0, le=10)
    highway_percentage: float = Field(ge=0, le=100)


class ParsedPreferences(CamelModel):
    """How the natural-language preference was interpreted."""

    weights: dict[str, float]
    recognized_terms: list[str]
    unrecognized_terms: list[str]
    confidence: float = Field(ge=0, le=1)
    note: str


class RankedRoute(CamelModel):
    route_id: str
    rank: int
    score: float
    explanation: str


class RankRequest(CamelModel):
    preference: str = Field(min_length=1, max_length=500)
    candidates: list[CandidateRoute]


class RankResponse(CamelModel):
    provider: str
    parsed_preferences: ParsedPreferences
    results: list[RankedRoute]
    recommended_route_id: str | None
