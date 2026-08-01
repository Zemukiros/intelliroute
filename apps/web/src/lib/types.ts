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

export interface ApiProblem {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details?: Record<string, unknown>;
}
