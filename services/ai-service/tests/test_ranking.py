"""Scoring and ranking tests over the sample-network candidate fixtures."""

import pytest
from pydantic import ValidationError

from app.models import CandidateRoute
from app.parsing import parse_preference
from app.ranking import rank_routes


def top(candidates, text):
    return rank_routes(candidates, parse_preference(text).weights)[0].route_id


class TestPreferenceOutcomes:
    def test_fastest_picks_highway_route(self, candidates):
        assert top(candidates, "fastest route") == "route-2"

    def test_safest_picks_highway_route(self, candidates):
        assert top(candidates, "safest route") == "route-2"

    def test_avoid_tolls_picks_toll_free(self, candidates):
        assert top(candidates, "avoid tolls") == "route-3"

    def test_scenic_picks_scenic(self, candidates):
        assert top(candidates, "prefer scenic roads") == "route-3"

    def test_avoid_highways_picks_highway_free(self, candidates):
        assert top(candidates, "avoid highways") == "route-3"

    def test_minimize_distance_picks_shortest(self, candidates):
        assert top(candidates, "minimize distance") == "route-1"

    def test_combined_safest_no_tolls(self, candidates):
        assert top(candidates, "choose the safest route and avoid tolls") == "route-3"


class TestRankingMechanics:
    def test_all_routes_ranked_with_consecutive_ranks(self, candidates):
        results = rank_routes(candidates, {"time": 1.0})
        assert [r.rank for r in results] == [1, 2, 3]
        assert {r.route_id for r in results} == {"route-1", "route-2", "route-3"}

    def test_deterministic_ordering(self, candidates):
        weights = parse_preference("balance time, safety, and cost").weights
        first = [r.route_id for r in rank_routes(candidates, weights)]
        second = [r.route_id for r in rank_routes(candidates, weights)]
        assert first == second

    def test_tie_broken_by_route_id(self, candidates):
        # Identical twin candidates force a score tie.
        twins = [
            candidates[0].model_copy(update={"route_id": "route-b"}),
            candidates[0].model_copy(update={"route_id": "route-a"}),
        ]
        results = rank_routes(twins, {"time": 1.0})
        assert [r.route_id for r in results] == ["route-a", "route-b"]
        assert results[0].score == results[1].score

    def test_indifferent_criterion_gives_equal_scores(self, candidates):
        same_toll = [c.model_copy(update={"toll_cost": 2.0}) for c in candidates]
        results = rank_routes(same_toll, {"toll": 1.0})
        assert len({r.score for r in results}) == 1

    def test_empty_candidate_list(self):
        assert rank_routes([], {"time": 1.0}) == []

    def test_no_active_weights_falls_back_to_distance(self, candidates):
        results = rank_routes(candidates, {})
        assert results[0].route_id == "route-1"

    def test_scores_bounded_zero_to_one(self, candidates):
        for text in ["fastest", "avoid tolls and highways", "scenic and safe"]:
            for r in rank_routes(candidates, parse_preference(text).weights):
                assert 0.0 <= r.score <= 1.0


class TestExplanations:
    def test_top_route_explains_best_metrics(self, candidates):
        results = rank_routes(candidates, parse_preference("avoid tolls").weights)
        assert "Best match" in results[0].explanation
        assert "lowest toll cost" in results[0].explanation
        assert "A -> C -> E -> F" in results[0].explanation

    def test_lower_ranks_labelled(self, candidates):
        results = rank_routes(candidates, parse_preference("fastest").weights)
        assert results[1].explanation.startswith("Ranked #2")
        assert results[2].explanation.startswith("Ranked #3")


class TestInvalidRouteData:
    def test_negative_distance_rejected(self):
        with pytest.raises(ValidationError):
            CandidateRoute(
                route_id="bad", path=["A"], total_distance_km=-1,
                travel_time_minutes=1, toll_cost=0, safety_score=5,
                scenic_score=5, highway_percentage=0)

    def test_out_of_range_safety_rejected(self):
        with pytest.raises(ValidationError):
            CandidateRoute(
                route_id="bad", path=["A"], total_distance_km=1,
                travel_time_minutes=1, toll_cost=0, safety_score=11,
                scenic_score=5, highway_percentage=0)
