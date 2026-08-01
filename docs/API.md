# IntelliRoute API Reference

Base URL (local): `http://localhost:8080`

## POST /api/routes/calculate

Calculates the shortest route between two nodes of the road network.

### Request

```json
{
  "origin": "A",
  "destination": "F"
}
```

| Field | Type | Rules |
|---|---|---|
| `origin` | string | required, non-blank, ≤ 32 chars, must be a known node id |
| `destination` | string | required, non-blank, ≤ 32 chars, must be a known node id |

### Success — 200 OK

```json
{
  "origin": "A",
  "destination": "F",
  "path": ["A", "C", "D", "F"],
  "totalDistance": 14.0,
  "visitedNodes": 6,
  "executionTimeMs": 0.012
}
```

| Field | Meaning |
|---|---|
| `path` | Node ids from origin to destination inclusive |
| `totalDistance` | Sum of edge weights (km) along the path |
| `visitedNodes` | Nodes settled by Dijkstra before terminating — search effort |
| `executionTimeMs` | Measured wall-clock time of the engine call (`System.nanoTime()`), rounded to 3 decimals |

### Errors

All errors share one shape:

```json
{
  "timestamp": "2026-08-01T15:30:00Z",
  "status": 404,
  "error": "UNKNOWN_NODE",
  "message": "Unknown origin node: 'Z'",
  "details": { "nodeId": "Z", "role": "origin" }
}
```

| Status | `error` | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Blank/oversized fields; `details` maps field → message |
| 400 | `MALFORMED_REQUEST` | Missing or non-JSON body |
| 404 | `UNKNOWN_NODE` | Origin or destination id not in the network |
| 422 | `ROUTE_UNREACHABLE` | Both nodes exist but no path connects them |

### Examples

```bash
# Success
curl -X POST localhost:8080/api/routes/calculate \
  -H "Content-Type: application/json" -d '{"origin":"A","destination":"F"}'

# Unreachable island destination -> 422
curl -X POST localhost:8080/api/routes/calculate \
  -H "Content-Type: application/json" -d '{"origin":"A","destination":"J"}'

# Unknown node -> 404
curl -X POST localhost:8080/api/routes/calculate \
  -H "Content-Type: application/json" -d '{"origin":"Z","destination":"F"}'
```

## POST /api/routes/alternatives

Generates up to N distinct loopless candidate routes (Yen's algorithm),
shortest first, each with aggregated road metadata.

### Request

```json
{ "origin": "A", "destination": "F", "maximumRoutes": 3 }
```

`maximumRoutes` is optional (default 3, bounds 1–5).

### Success — 200 OK

```json
{
  "origin": "A",
  "destination": "F",
  "requestedRoutes": 3,
  "returnedRoutes": 3,
  "routes": [
    {
      "routeId": "route-1",
      "path": ["A", "C", "D", "F"],
      "totalDistanceKm": 14.0,
      "travelTimeMinutes": 16.5,
      "tollCost": 3.0,
      "safetyScore": 6.71,
      "scenicScore": 4.21,
      "highwayPercentage": 35.7
    }
  ],
  "executionTimeMs": 0.9,
  "dijkstraInvocations": 7,
  "totalVisitedNodes": 41
}
```

`safetyScore`/`scenicScore` are distance-weighted averages (0–10);
`highwayPercentage` is the share of distance on highway segments. Fewer
routes than requested are returned when the graph has fewer distinct
loopless paths. Errors mirror `/calculate` (404 unknown node, 422
unreachable, 400 validation).

## POST /api/routes/recommend

Generates alternatives and ranks them against a natural-language
preference via the ranking service. Never fails when the ranking service
is down — it degrades to a local distance-ordered fallback and says so.

### Request

```json
{
  "origin": "A",
  "destination": "F",
  "preference": "Choose the safest route and avoid tolls",
  "maximumRoutes": 3
}
```

`preference` is required (≤ 500 chars).

### Success — 200 OK (abridged)

```json
{
  "origin": "A",
  "destination": "F",
  "preference": "Choose the safest route and avoid tolls",
  "provider": "local-deterministic-v1",
  "fallbackUsed": false,
  "parsedPreferences": {
    "weights": { "safety": 0.5, "toll": 0.5 },
    "recognizedTerms": ["avoid tolls", "safest"],
    "unrecognizedTerms": [],
    "confidence": 0.67,
    "note": "Interpreted preference across: safety, toll"
  },
  "candidates": [ { "routeId": "route-1", "...": "as in /alternatives" } ],
  "rankedRoutes": [
    { "routeId": "route-3", "rank": 1, "score": 0.52,
      "explanation": "Best match for the stated preference: lowest toll cost (0.00 in tolls); ..." }
  ],
  "recommendedRouteId": "route-3",
  "routingTimeMs": 0.8,
  "rankingTimeMs": 6.2
}
```

When the ranking service is unreachable: `fallbackUsed` is `true`,
`provider` is `java-local-fallback`, routes are distance-ordered, and
`parsedPreferences.confidence` is 0 with an explanatory note. Timings are
measured per request. See [ROUTE_RANKING.md](ROUTE_RANKING.md) for the
provider's parsing and scoring rules.

## GET /api/routes/network

Returns the road network for visualization. Two-way roads are emitted
once; closed roads are included with `"open": false` (never routed).

```json
{
  "nodes": [{ "id": "A", "name": "Ashford", "x": 80.0, "y": 210.0 }],
  "edges": [{
    "from": "A", "to": "B", "distanceKm": 5.0,
    "travelTimeMinutes": 4.6, "roadType": "ARTERIAL",
    "tollCost": 0.0, "safetyScore": 7.0, "scenicScore": 4.0, "open": true
  }]
}
```

Coordinates are abstract layout positions, not geographic coordinates.

## GET /actuator/health

Standard Spring Boot health probe: `{"status":"UP"}`, including a
`rankingService` component reporting `remote-ranking` or `local-fallback`
mode. Used by Docker Compose and CI.

## Ranking service (port 8090)

`GET /health` — `{"status": "ok", "provider": "local-deterministic-v1"}`.
`POST /rank` — ranks candidates against a preference; the Java API is its
only intended caller. Contract and rules: [ROUTE_RANKING.md](ROUTE_RANKING.md).
