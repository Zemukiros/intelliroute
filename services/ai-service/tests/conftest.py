"""Shared fixtures: the three A→F candidates from the Java sample network."""

import pytest

from app.models import CandidateRoute


@pytest.fixture
def candidates() -> list[CandidateRoute]:
    return [
        CandidateRoute(
            route_id="route-1", path=["A", "C", "D", "F"],
            total_distance_km=14.0, travel_time_minutes=16.5, toll_cost=3.00,
            safety_score=6.71, scenic_score=4.21, highway_percentage=35.7),
        CandidateRoute(
            route_id="route-2", path=["A", "B", "D", "F"],
            total_distance_km=17.0, travel_time_minutes=11.8, toll_cost=5.50,
            safety_score=8.41, scenic_score=2.59, highway_percentage=70.6),
        CandidateRoute(
            route_id="route-3", path=["A", "C", "E", "F"],
            total_distance_km=18.0, travel_time_minutes=22.8, toll_cost=0.00,
            safety_score=6.78, scenic_score=8.33, highway_percentage=0.0),
    ]
