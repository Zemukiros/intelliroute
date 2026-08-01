"""Reproducible benchmark for the deterministic preference-ranking provider.

Methodology: fixed seed, generated candidate sets, warm-up pass, per-call
wall-clock timing via time.perf_counter_ns, reporting average/median/p95.

Run from services/ai-service:  python3 scripts/benchmark_ranking.py
"""

from __future__ import annotations

import platform
import random
import statistics
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.models import CandidateRoute  # noqa: E402
from app.providers import DeterministicPreferenceRankingProvider  # noqa: E402

SEED = 42
WARMUP = 2_000
RUNS = 10_000
PREFERENCES = [
    "fastest route",
    "choose the safest route and avoid tolls",
    "prefer scenic roads even if slightly longer",
    "balance time, safety, and cost",
    "avoid highways and tolls",
]


def make_candidates(rng: random.Random, count: int) -> list[CandidateRoute]:
    candidates = []
    for i in range(count):
        distance = round(rng.uniform(5, 60), 1)
        candidates.append(CandidateRoute(
            route_id=f"route-{i + 1}",
            path=[f"N{j}" for j in range(rng.randint(2, 8))],
            total_distance_km=distance,
            travel_time_minutes=round(distance * rng.uniform(0.8, 2.0), 1),
            toll_cost=round(rng.choice([0, 0, 2.5, 5.0]), 2),
            safety_score=round(rng.uniform(3, 10), 2),
            scenic_score=round(rng.uniform(0, 10), 2),
            highway_percentage=round(rng.uniform(0, 100), 1),
        ))
    return candidates


def main() -> None:
    rng = random.Random(SEED)
    provider = DeterministicPreferenceRankingProvider()
    candidate_sets = [make_candidates(rng, rng.randint(3, 5)) for _ in range(100)]

    print("# IntelliRoute ranking benchmark")
    print(f"environment: python={platform.python_version()},"
          f" os={platform.system()} {platform.machine()}")
    print(f"seed={SEED}, warmup={WARMUP}, runs={RUNS},"
          f" candidate sets=100 (3-5 routes each)\n")

    for _ in range(WARMUP):
        provider.rank(PREFERENCES[_ % len(PREFERENCES)],
                      candidate_sets[_ % len(candidate_sets)])

    samples_ns: list[int] = []
    for i in range(RUNS):
        preference = PREFERENCES[i % len(PREFERENCES)]
        candidates = candidate_sets[i % len(candidate_sets)]
        start = time.perf_counter_ns()
        provider.rank(preference, candidates)
        samples_ns.append(time.perf_counter_ns() - start)

    samples_us = sorted(ns / 1_000 for ns in samples_ns)
    avg = statistics.fmean(samples_us)
    median = statistics.median(samples_us)
    p95 = samples_us[int(len(samples_us) * 0.95) - 1]
    print(f"parse + rank (full provider call): "
          f"avg={avg:.1f} us  median={median:.1f} us  p95={p95:.1f} us")


if __name__ == "__main__":
    main()
