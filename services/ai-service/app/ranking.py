"""Deterministic route scoring and explanation generation.

Scores each candidate route against parsed preference weights using
min-max-normalized utilities, then ranks with deterministic tie-breaking.
"""

from __future__ import annotations

from .models import CandidateRoute, RankedRoute

# criterion -> (attribute, higher_is_better, display name, unit formatter)
_CRITERIA: dict[str, tuple[str, bool, str]] = {
    "time": ("travel_time_minutes", False, "travel time"),
    "distance": ("total_distance_km", False, "distance"),
    "toll": ("toll_cost", False, "toll cost"),
    "safety": ("safety_score", True, "safety"),
    "scenic": ("scenic_score", True, "scenery"),
    "avoidHighways": ("highway_percentage", False, "highway share"),
}


def _format_value(criterion: str, value: float) -> str:
    if criterion == "time":
        return f"{value:.1f} min"
    if criterion == "distance":
        return f"{value:.1f} km"
    if criterion == "toll":
        return f"{value:.2f} in tolls"
    if criterion == "avoidHighways":
        return f"{value:.0f}% highway"
    return f"{value:.1f}/10 {_CRITERIA[criterion][2]}"


def _utilities(candidates: list[CandidateRoute], criterion: str) -> list[float]:
    attribute, higher_better, _ = _CRITERIA[criterion]
    values = [getattr(c, attribute) for c in candidates]
    low, high = min(values), max(values)
    if high == low:
        return [1.0] * len(values)  # criterion cannot differentiate
    if higher_better:
        return [(v - low) / (high - low) for v in values]
    return [(high - v) / (high - low) for v in values]


def rank_routes(candidates: list[CandidateRoute],
                weights: dict[str, float]) -> list[RankedRoute]:
    """Ranks candidates by weighted normalized utility.

    Deterministic: score ties are broken by routeId (ascending), so the same
    input always yields the same ordering.
    """
    if not candidates:
        return []

    active = {c: w for c, w in weights.items() if c in _CRITERIA and w > 0}
    if not active:
        active = {"distance": 1.0}

    utilities_by_criterion = {c: _utilities(candidates, c) for c in active}
    scores: list[float] = []
    for i in range(len(candidates)):
        score = sum(w * utilities_by_criterion[c][i] for c, w in active.items())
        scores.append(round(score, 4))

    order = sorted(range(len(candidates)),
                   key=lambda i: (-scores[i], candidates[i].route_id))

    results: list[RankedRoute] = []
    for rank_position, i in enumerate(order, start=1):
        results.append(RankedRoute(
            route_id=candidates[i].route_id,
            rank=rank_position,
            score=scores[i],
            explanation=_explain(candidates, i, active, utilities_by_criterion,
                                 rank_position),
        ))
    return results


def _explain(candidates: list[CandidateRoute], index: int,
             active: dict[str, float],
             utilities: dict[str, list[float]], rank_position: int) -> str:
    """Human-readable, deterministic explanation for one route's rank."""
    route = candidates[index]
    # Describe criteria in descending weight order (alphabetical on ties).
    ordered = sorted(active.items(), key=lambda kv: (-kv[1], kv[0]))
    fragments: list[str] = []
    for criterion, _weight in ordered:
        attribute, _higher, _label = _CRITERIA[criterion]
        value = getattr(route, attribute)
        utility = utilities[criterion][index]
        if utility >= 0.999:
            best_word = "highest" if _CRITERIA[criterion][1] else "lowest"
            if len(set(utilities[criterion])) == 1:
                fragments.append(
                    f"{_CRITERIA[criterion][2]} is equal across routes"
                    f" ({_format_value(criterion, value)})")
            else:
                fragments.append(
                    f"{best_word} {_CRITERIA[criterion][2]}"
                    f" ({_format_value(criterion, value)})")
        else:
            fragments.append(f"{_format_value(criterion, value)}")

    lead = ("Best match for the stated preference: "
            if rank_position == 1 else f"Ranked #{rank_position}: ")
    summary = (f" Path {' -> '.join(route.path)} covers"
               f" {route.total_distance_km:.1f} km"
               f" in ~{route.travel_time_minutes:.1f} min.")
    return lead + "; ".join(fragments) + "." + summary
