"""Deterministic natural-language preference parsing.

Turns free text like "choose the safest route and avoid tolls" into
normalized criterion weights. Pure keyword/synonym/phrase matching — no
machine learning, no external calls, fully reproducible.

Criteria (weight keys):
    time            minimize travel time
    distance        minimize total distance
    toll            minimize toll cost
    safety          maximize safety score
    scenic          maximize scenic score
    avoidHighways   minimize highway percentage
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

# Phrases are matched before single words so "avoid highways" is consumed
# as one intent rather than as an unknown "avoid" plus a bare "highways".
_PHRASE_RULES: list[tuple[str, str]] = [
    # criterion, regex (matched on the normalized text)
    ("avoidHighways", r"\b(avoid|no|without|skip|stay off|keep off|off)\s+(the\s+)?(highways?|motorways?|freeways?|interstates?)\b"),
    ("avoidHighways", r"\b(highway|motorway|freeway|interstate)[- ]?free\b"),
    ("toll", r"\b(avoid|no|without|skip|minimi[sz]e)\s+(the\s+)?tolls?\b"),
    ("toll", r"\btoll[- ]?free\b"),
    ("safety", r"\b(avoid|minimi[sz]e)\s+(danger(ous)?|risk(y)?|accidents?)\b"),
    ("time", r"\b(minimi[sz]e|least|save|shortest)\s+(travel\s+)?time\b"),
    ("distance", r"\b(minimi[sz]e|least|shortest)\s+distance\b"),
]

_WORD_RULES: dict[str, list[str]] = {
    "time": ["fast", "faster", "fastest", "quick", "quicker", "quickest",
             "quickly", "speed", "speedy", "rapid", "rapidly", "hurry", "time"],
    "distance": ["short", "shorter", "shortest", "direct", "distance"],
    "toll": ["toll", "tolls", "cheap", "cheaper", "cheapest", "cost", "costs",
             "fee", "fees", "money", "budget", "inexpensive"],
    "safety": ["safe", "safer", "safest", "safety", "secure", "securely",
               "carefully", "cautious"],
    "scenic": ["scenic", "scenery", "beautiful", "pretty", "picturesque",
               "views", "view", "landscape", "landscapes", "sightseeing",
               "countryside"],
}

# Words that signal "weight the mentioned criteria evenly" (or a fully
# balanced default when no criteria are mentioned at all).
# Note: "even" is deliberately absent — it appears in phrases like
# "even if it is slightly longer" where it carries no balancing intent.
_BALANCE_WORDS = {"balance", "balanced", "balancing", "mix", "mixed",
                  "tradeoff", "compromise", "evenly"}

# Common words carrying no preference signal; never reported as unrecognized.
_STOPWORDS = {
    "a", "an", "the", "and", "or", "but", "of", "to", "in", "on", "for",
    "with", "without", "is", "it", "its", "i", "me", "my", "we", "us",
    "our", "you", "your", "please", "want", "need", "would", "like",
    "prefer", "preferred", "give", "choose", "pick", "find", "take",
    "go", "get", "route", "routes", "road", "roads", "way", "path",
    "trip", "drive", "driving", "one", "if", "even", "slightly", "bit",
    "little", "longer", "more", "most", "less", "least", "avoid",
    "minimize", "minimise", "maximize", "maximise", "possible", "as",
    "am", "be", "that", "this", "them", "then", "than", "so", "very",
    "really", "just", "still", "also", "though", "however", "will",
    "can", "could", "should", "must", "me", "not",
}

_BALANCED_DEFAULT = {"time": 0.25, "distance": 0.25, "toll": 0.25, "safety": 0.25}


@dataclass
class ParseOutcome:
    weights: dict[str, float]
    recognized_terms: list[str] = field(default_factory=list)
    unrecognized_terms: list[str] = field(default_factory=list)
    confidence: float = 0.0
    note: str = ""


def _normalize(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[^a-z0-9\s-]", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def parse_preference(text: str) -> ParseOutcome:
    """Parses free text into normalized criterion weights (sum = 1.0).

    Deterministic: identical input always yields identical output. Unclear
    input falls back to a documented balanced default with confidence 0.
    """
    normalized = _normalize(text)
    raw_scores: dict[str, float] = {}
    recognized: list[str] = []
    consumed = normalized

    # 1. Phrase rules first (consume matched text so word rules don't double-count).
    for criterion, pattern in _PHRASE_RULES:
        for match in re.finditer(pattern, consumed):
            raw_scores[criterion] = raw_scores.get(criterion, 0.0) + 1.0
            recognized.append(match.group(0).strip())
        consumed = re.sub(pattern, " ", consumed)

    # 2. Single-word synonym rules.
    tokens = consumed.split()
    matched_tokens: set[int] = set()
    for criterion, words in _WORD_RULES.items():
        for i, token in enumerate(tokens):
            if token in words:
                raw_scores[criterion] = raw_scores.get(criterion, 0.0) + 1.0
                recognized.append(token)
                matched_tokens.add(i)

    # 3. Balance words.
    balance_requested = False
    for i, token in enumerate(tokens):
        if token in _BALANCE_WORDS and i not in matched_tokens:
            balance_requested = True
            recognized.append(token)
            matched_tokens.add(i)

    unrecognized = sorted({
        t for i, t in enumerate(tokens)
        if i not in matched_tokens and t not in _STOPWORDS and not t.isdigit()
    })

    content_token_count = len([t for t in _normalize(text).split() if t not in _STOPWORDS])
    confidence = round(len(recognized) / content_token_count, 2) if content_token_count else 0.0
    confidence = min(confidence, 1.0)

    if not raw_scores:
        if balance_requested:
            return ParseOutcome(
                weights=dict(_BALANCED_DEFAULT),
                recognized_terms=recognized,
                unrecognized_terms=unrecognized,
                confidence=confidence,
                note="Balanced preference requested — weighting time, distance,"
                     " toll, and safety evenly.",
            )
        return ParseOutcome(
            weights=dict(_BALANCED_DEFAULT),
            recognized_terms=[],
            unrecognized_terms=unrecognized,
            confidence=0.0,
            note="No supported preference terms recognized — using a balanced"
                 " default (time, distance, toll, safety weighted evenly)."
                 " Supported ideas: fastest, shortest, avoid tolls, safest,"
                 " scenic, avoid highways, balanced.",
        )

    if balance_requested:
        # "Balance X, Y and Z" → weight the mentioned criteria evenly.
        raw_scores = {criterion: 1.0 for criterion in raw_scores}

    total = sum(raw_scores.values())
    weights = {c: round(v / total, 4) for c, v in sorted(raw_scores.items())}

    note = "Interpreted preference across: " + ", ".join(sorted(weights))
    if unrecognized:
        note += ". Ignored unsupported terms: " + ", ".join(unrecognized)
    return ParseOutcome(
        weights=weights,
        recognized_terms=recognized,
        unrecognized_terms=unrecognized,
        confidence=confidence,
        note=note,
    )
