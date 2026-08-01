import type {
  ApiProblem,
  Network,
  RecommendResult,
  RouteResult,
} from "./types";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/** Error carrying the structured problem body returned by the API. */
export class ApiError extends Error {
  readonly problem: ApiProblem | null;

  constructor(message: string, problem: ApiProblem | null) {
    super(message);
    this.name = "ApiError";
    this.problem = problem;
  }
}

async function parseProblem(response: Response): Promise<never> {
  let problem: ApiProblem | null = null;
  try {
    problem = (await response.json()) as ApiProblem;
  } catch {
    // Non-JSON error body; fall through with a generic message.
  }
  throw new ApiError(
    problem?.message ?? `Request failed with status ${response.status}`,
    problem,
  );
}

export async function fetchNetwork(): Promise<Network> {
  const response = await fetch(`${API_BASE}/api/routes/network`);
  if (!response.ok) {
    return parseProblem(response);
  }
  return (await response.json()) as Network;
}

export async function calculateRoute(
  origin: string,
  destination: string,
): Promise<RouteResult> {
  const response = await fetch(`${API_BASE}/api/routes/calculate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ origin, destination }),
  });
  if (!response.ok) {
    return parseProblem(response);
  }
  return (await response.json()) as RouteResult;
}

export async function recommendRoutes(
  origin: string,
  destination: string,
  preference: string,
  maximumRoutes: number,
): Promise<RecommendResult> {
  const response = await fetch(`${API_BASE}/api/routes/recommend`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ origin, destination, preference, maximumRoutes }),
  });
  if (!response.ok) {
    return parseProblem(response);
  }
  return (await response.json()) as RecommendResult;
}
