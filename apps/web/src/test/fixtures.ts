import type { Network, RecommendResult } from "@/lib/types";

export const network: Network = {
  nodes: [
    { id: "A", name: "Ashford", x: 80, y: 210 },
    { id: "B", name: "Brookhaven", x: 240, y: 80 },
    { id: "C", name: "Cedarville", x: 260, y: 300 },
    { id: "D", name: "Dunmore", x: 450, y: 190 },
    { id: "E", name: "Eastvale", x: 470, y: 380 },
    { id: "F", name: "Fairmont", x: 650, y: 280 },
  ],
  edges: [
    { from: "A", to: "C", distanceKm: 4, travelTimeMinutes: 6, roadType: "LOCAL", tollCost: 0, safetyScore: 6, scenicScore: 6, open: true },
    { from: "C", to: "D", distanceKm: 5, travelTimeMinutes: 7.5, roadType: "LOCAL", tollCost: 0, safetyScore: 5, scenicScore: 5, open: true },
    { from: "D", to: "F", distanceKm: 5, travelTimeMinutes: 3, roadType: "HIGHWAY", tollCost: 3, safetyScore: 9, scenicScore: 2, open: true },
    { from: "B", to: "E", distanceKm: 9, travelTimeMinutes: 13.5, roadType: "LOCAL", tollCost: 0, safetyScore: 5, scenicScore: 5, open: false },
  ],
};

export const recommendResult: RecommendResult = {
  origin: "A",
  destination: "F",
  preference: "avoid tolls",
  provider: "local-deterministic-v1",
  fallbackUsed: false,
  parsedPreferences: {
    weights: { toll: 1.0 },
    recognizedTerms: ["avoid tolls"],
    unrecognizedTerms: [],
    confidence: 1.0,
    note: "Interpreted preference across: toll",
  },
  candidates: [
    {
      routeId: "route-1",
      path: ["A", "C", "D", "F"],
      totalDistanceKm: 14,
      travelTimeMinutes: 16.5,
      tollCost: 3,
      safetyScore: 6.71,
      scenicScore: 4.21,
      highwayPercentage: 35.7,
    },
    {
      routeId: "route-3",
      path: ["A", "C", "E", "F"],
      totalDistanceKm: 18,
      travelTimeMinutes: 22.8,
      tollCost: 0,
      safetyScore: 6.78,
      scenicScore: 8.33,
      highwayPercentage: 0,
    },
  ],
  rankedRoutes: [
    { routeId: "route-3", rank: 1, score: 1.0, explanation: "Best match: lowest toll cost (0.00 in tolls)." },
    { routeId: "route-1", rank: 2, score: 0.45, explanation: "Ranked #2: 3.00 in tolls." },
  ],
  recommendedRouteId: "route-3",
  routingTimeMs: 0.42,
  rankingTimeMs: 3.1,
};

export function okJson(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body,
  } as unknown as Response;
}

export function errorJson(status: number, body: unknown): Response {
  return {
    ok: false,
    status,
    json: async () => body,
  } as unknown as Response;
}
