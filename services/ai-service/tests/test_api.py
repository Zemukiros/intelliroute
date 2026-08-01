"""HTTP contract tests for the FastAPI app (camelCase wire format)."""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _payload(candidates):
    return [c.model_dump(by_alias=True) for c in candidates]


def test_health_reports_provider():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok",
                               "provider": "local-deterministic-v1"}


def test_rank_full_contract(candidates):
    response = client.post("/rank", json={
        "preference": "Choose the safest route and avoid tolls",
        "candidates": _payload(candidates),
    })
    assert response.status_code == 200
    body = response.json()
    assert body["provider"] == "local-deterministic-v1"
    assert body["recommendedRouteId"] == "route-3"
    assert body["parsedPreferences"]["weights"] == {"safety": 0.5, "toll": 0.5}
    assert body["parsedPreferences"]["confidence"] > 0
    assert [r["rank"] for r in body["results"]] == [1, 2, 3]
    assert all(r["explanation"] for r in body["results"])


def test_rank_unknown_preference_uses_balanced_default(candidates):
    response = client.post("/rank", json={
        "preference": "blorptastic voyage",
        "candidates": _payload(candidates),
    })
    assert response.status_code == 200
    body = response.json()
    assert body["parsedPreferences"]["confidence"] == 0.0
    assert "balanced default" in body["parsedPreferences"]["note"]
    assert body["recommendedRouteId"] is not None


def test_rank_empty_candidates_returns_no_recommendation():
    response = client.post("/rank", json={"preference": "fastest",
                                          "candidates": []})
    assert response.status_code == 200
    body = response.json()
    assert body["results"] == []
    assert body["recommendedRouteId"] is None


def test_rank_invalid_candidate_rejected(candidates):
    bad = _payload(candidates)
    bad[0]["totalDistanceKm"] = -5
    response = client.post("/rank", json={"preference": "fastest",
                                          "candidates": bad})
    assert response.status_code == 422


def test_rank_blank_preference_rejected(candidates):
    response = client.post("/rank", json={"preference": "",
                                          "candidates": _payload(candidates)})
    assert response.status_code == 422
