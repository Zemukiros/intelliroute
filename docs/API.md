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

## GET /api/routes/network

Returns the road network for visualization. Two-way roads are emitted once.

```json
{
  "nodes": [{ "id": "A", "name": "Ashford", "x": 80.0, "y": 210.0 }],
  "edges": [{ "from": "A", "to": "B", "distanceKm": 5.0 }]
}
```

Coordinates are abstract layout positions, not geographic coordinates.

## GET /actuator/health

Standard Spring Boot health probe: `{"status":"UP"}`. Used by Docker Compose health checks and the CI smoke test.

## AI service (scaffold, port 8090)

`POST /rank` — re-ranks candidate routes against a natural-language preference. Milestone 1 uses a deterministic mock provider; the contract is stable so a real provider can be swapped in without breaking clients. See `services/ai-service/README.md`.
