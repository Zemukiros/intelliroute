"""Preference-parsing tests: individual terms, synonyms, combinations,
negations, balance, unknown and ambiguous input, weight normalization."""

import math

import pytest

from app.parsing import parse_preference


def weights_sum_to_one(weights: dict[str, float]) -> bool:
    return math.isclose(sum(weights.values()), 1.0, abs_tol=0.001)


class TestIndividualPreferences:
    def test_fastest(self):
        outcome = parse_preference("find the fastest route")
        assert outcome.weights == {"time": 1.0}
        assert outcome.confidence > 0

    def test_safest(self):
        assert parse_preference("give me the safest route").weights == {"safety": 1.0}

    def test_avoid_tolls(self):
        outcome = parse_preference("avoid tolls")
        assert outcome.weights == {"toll": 1.0}
        assert "avoid tolls" in outcome.recognized_terms

    def test_avoid_highways(self):
        outcome = parse_preference("avoid highways")
        assert outcome.weights == {"avoidHighways": 1.0}

    def test_scenic(self):
        assert parse_preference("prefer scenic roads").weights == {"scenic": 1.0}

    def test_minimize_distance(self):
        assert parse_preference("minimize distance").weights == {"distance": 1.0}


class TestSynonyms:
    @pytest.mark.parametrize("text", ["quickest way", "a rapid trip", "speedy route"])
    def test_time_synonyms(self, text):
        assert parse_preference(text).weights == {"time": 1.0}

    @pytest.mark.parametrize("text", ["no tolls please", "toll-free please", "cheapest route"])
    def test_toll_synonyms(self, text):
        assert parse_preference(text).weights == {"toll": 1.0}

    @pytest.mark.parametrize("text", [
        "stay off the freeways", "no motorways", "without interstates", "highway-free"])
    def test_highway_synonyms(self, text):
        assert parse_preference(text).weights == {"avoidHighways": 1.0}

    @pytest.mark.parametrize("text", ["beautiful views", "picturesque countryside"])
    def test_scenic_synonyms(self, text):
        assert parse_preference(text).weights == {"scenic": 1.0}


class TestCombinedPreferences:
    def test_two_criteria_split_evenly(self):
        outcome = parse_preference("choose the safest route and avoid tolls")
        assert outcome.weights == {"safety": 0.5, "toll": 0.5}

    def test_balance_of_three(self):
        outcome = parse_preference("balance time, safety, and cost")
        assert set(outcome.weights) == {"time", "safety", "toll"}
        for w in outcome.weights.values():
            assert math.isclose(w, 1 / 3, abs_tol=0.001)

    def test_scenic_even_if_longer(self):
        outcome = parse_preference("prefer a scenic route even if it is slightly longer")
        assert outcome.weights == {"scenic": 1.0}

    def test_repeated_emphasis_increases_weight(self):
        outcome = parse_preference("fast fast fast but cheap")
        assert outcome.weights["time"] > outcome.weights["toll"]
        assert weights_sum_to_one(outcome.weights)


class TestUnknownAndAmbiguous:
    def test_unknown_input_falls_back_balanced(self):
        outcome = parse_preference("purple elephant spaghetti")
        assert weights_sum_to_one(outcome.weights)
        assert set(outcome.weights) == {"time", "distance", "toll", "safety"}
        assert outcome.confidence == 0.0
        assert "balanced default" in outcome.note
        assert "elephant" in outcome.unrecognized_terms

    def test_partial_recognition_keeps_recognized_part(self):
        outcome = parse_preference("safest zorbulous route")
        assert outcome.weights == {"safety": 1.0}
        assert "zorbulous" in outcome.unrecognized_terms
        assert "Ignored unsupported terms" in outcome.note

    def test_empty_after_stopwords(self):
        outcome = parse_preference("please give me a route")
        assert outcome.confidence == 0.0
        assert weights_sum_to_one(outcome.weights)


class TestNormalizationAndDeterminism:
    @pytest.mark.parametrize("text", [
        "fastest", "avoid tolls and highways", "safe scenic cheap quick short"])
    def test_weights_always_normalized(self, text):
        assert weights_sum_to_one(parse_preference(text).weights)

    def test_deterministic(self):
        a = parse_preference("Safest route, avoid tolls, scenic if possible")
        b = parse_preference("Safest route, avoid tolls, scenic if possible")
        assert a.weights == b.weights
        assert a.recognized_terms == b.recognized_terms
        assert a.confidence == b.confidence

    def test_case_and_punctuation_insensitive(self):
        assert (parse_preference("AVOID TOLLS!").weights
                == parse_preference("avoid tolls").weights)
