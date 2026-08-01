/** Shared API contract types mirroring the Spring Boot DTOs. */

export interface NetworkNode {
  id: string;
  name: string;
  x: number;
  y: number;
}

export interface NetworkEdge {
  from: string;
  to: string;
  distanceKm: number;
  travelTimeMinutes: number;
  roadType: "HIGHWAY" | "ARTERIAL" | "LOCAL" | "SCENIC";
  tollCost: number;
  safetyScore: number;
  scenicScore: number;
  open: boolean;
}

export interface Network {
  nodes: NetworkNode[];
  edges: NetworkEdge[];
}

export interface RouteResult {
  origin: string;
  destination: string;
  path: string[];
  totalDistance: number;
  visitedNodes: number;
  executionTimeMs: number;
}

export interface RouteCandidate {
  routeId: string;
  path: string[];
  totalDistanceKm: number;
  travelTimeMinutes: number;
  tollCost: number;
  safetyScore: number;
  scenicScore: number;
  highwayPercentage: number;
}

export interface ParsedPreferences {
  weights: Record<string, number>;
  recognizedTerms: string[];
  unrecognizedTerms: string[];
  confidence: number;
  note: string;
}

export interface RankedRoute {
  routeId: string;
  rank: number;
  score: number;
  explanation: string;
}

export interface RecommendResult {
  origin: string;
  destination: string;
  preference: string;
  provider: string;
  fallbackUsed: boolean;
  parsedPreferences: ParsedPreferences;
  candidates: RouteCandidate[];
  rankedRoutes: RankedRoute[];
  recommendedRouteId: string;
  routingTimeMs: number;
  rankingTimeMs: number;
}

export interface ApiProblem {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details?: Record<string, unknown>;
}
