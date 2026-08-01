"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError, fetchNetwork, recommendRoutes } from "@/lib/api";
import type { Network, RecommendResult } from "@/lib/types";
import GraphView from "./GraphView";
import RouteCard from "./RouteCard";

type LoadState = "loading" | "ready" | "offline";

const EXAMPLE_PREFERENCES = [
  "Fastest route",
  "Safest route",
  "Avoid tolls",
  "Prefer scenic roads",
  "Avoid highways",
  "Balance time, safety, and cost",
];

/**
 * Interactive route intelligence planner: pick origin/destination, describe
 * a preference in plain language, and compare ranked candidate routes.
 */
export default function RoutePlanner() {
  const [network, setNetwork] = useState<Network | null>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [origin, setOrigin] = useState("A");
  const [destination, setDestination] = useState("F");
  const [preference, setPreference] = useState("Fastest route");
  const [maxRoutes, setMaxRoutes] = useState(3);
  const [result, setResult] = useState<RecommendResult | null>(null);
  const [selectedRouteId, setSelectedRouteId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const loadNetwork = useCallback(async () => {
    setLoadState("loading");
    try {
      setNetwork(await fetchNetwork());
      setLoadState("ready");
    } catch {
      setLoadState("offline");
    }
  }, []);

  useEffect(() => {
    void loadNetwork();
  }, [loadNetwork]);

  const onGenerate = async () => {
    if (!preference.trim()) {
      setValidationError("Describe a route preference first — or pick one of the examples.");
      return;
    }
    setValidationError(null);
    setLoading(true);
    setError(null);
    setResult(null);
    setSelectedRouteId(null);
    try {
      const data = await recommendRoutes(origin, destination, preference.trim(), maxRoutes);
      setResult(data);
      setSelectedRouteId(data.recommendedRouteId);
    } catch (e) {
      if (e instanceof ApiError && e.problem) {
        setError(`${e.problem.error}: ${e.problem.message}`);
      } else {
        setError("Could not reach the IntelliRoute API. Is the backend running on port 8080?");
      }
    } finally {
      setLoading(false);
    }
  };

  const shortestRouteId = useMemo(() => {
    if (!result || result.candidates.length === 0) return null;
    return result.candidates.reduce((best, c) =>
      c.totalDistanceKm < best.totalDistanceKm ? c : best,
    ).routeId;
  }, [result]);

  const selectedPath = useMemo(() => {
    if (!result || !selectedRouteId) return [];
    return result.candidates.find((c) => c.routeId === selectedRouteId)?.path ?? [];
  }, [result, selectedRouteId]);

  const rankedById = useMemo(
    () => new Map((result?.rankedRoutes ?? []).map((r) => [r.routeId, r])),
    [result],
  );

  const orderedCandidates = useMemo(() => {
    if (!result) return [];
    return [...result.candidates].sort((a, b) => {
      const ra = rankedById.get(a.routeId)?.rank ?? 99;
      const rb = rankedById.get(b.routeId)?.rank ?? 99;
      return ra - rb;
    });
  }, [result, rankedById]);

  if (loadState === "loading") {
    return (
      <p className="rounded-xl border border-slate-800 bg-panel p-6 text-slate-400">
        Loading road network…
      </p>
    );
  }

  if (loadState === "offline" || !network) {
    return (
      <div className="rounded-xl border border-amber-700/50 bg-amber-950/30 p-6">
        <h2 className="font-semibold text-amber-300">API not reachable</h2>
        <p className="mt-2 text-sm text-amber-100/80">
          The IntelliRoute API is not responding. Start it with{" "}
          <code className="rounded bg-black/40 px-1.5 py-0.5">mvn spring-boot:run</code>{" "}
          in <code className="rounded bg-black/40 px-1.5 py-0.5">services/api</code>, then retry.
        </p>
        <button
          onClick={() => void loadNetwork()}
          className="mt-4 rounded-lg bg-amber-500 px-4 py-2 text-sm font-semibold text-black hover:bg-amber-400"
        >
          Retry connection
        </button>
      </div>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
      {/* Control panel */}
      <div className="space-y-5 rounded-xl border border-slate-800 bg-panel p-6">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="origin" className="mb-1.5 block text-sm font-medium text-slate-300">
              Origin
            </label>
            <select
              id="origin"
              value={origin}
              onChange={(e) => setOrigin(e.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-surface px-3 py-2 text-slate-100 focus:border-accent focus:outline-none"
            >
              {network.nodes.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.id} — {n.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="destination" className="mb-1.5 block text-sm font-medium text-slate-300">
              Destination
            </label>
            <select
              id="destination"
              value={destination}
              onChange={(e) => setDestination(e.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-surface px-3 py-2 text-slate-100 focus:border-accent focus:outline-none"
            >
              {network.nodes.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.id} — {n.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label htmlFor="preference" className="mb-1.5 block text-sm font-medium text-slate-300">
            Route preference (plain language)
          </label>
          <textarea
            id="preference"
            value={preference}
            onChange={(e) => setPreference(e.target.value)}
            rows={2}
            maxLength={500}
            placeholder='e.g. "Choose the safest route and avoid tolls"'
            className="w-full resize-none rounded-lg border border-slate-700 bg-surface px-3 py-2 text-slate-100 placeholder:text-slate-600 focus:border-accent focus:outline-none"
          />
          <div className="mt-2 flex flex-wrap gap-1.5" aria-label="Example preferences">
            {EXAMPLE_PREFERENCES.map((example) => (
              <button
                key={example}
                type="button"
                onClick={() => setPreference(example)}
                className="rounded-full border border-slate-700 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent hover:text-accent"
              >
                {example}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label htmlFor="maxRoutes" className="mb-1.5 block text-sm font-medium text-slate-300">
            Maximum routes
          </label>
          <select
            id="maxRoutes"
            value={maxRoutes}
            onChange={(e) => setMaxRoutes(Number(e.target.value))}
            className="w-full rounded-lg border border-slate-700 bg-surface px-3 py-2 text-slate-100 focus:border-accent focus:outline-none"
          >
            {[1, 2, 3, 4, 5].map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>

        <button
          onClick={() => void onGenerate()}
          disabled={loading}
          className="w-full rounded-lg bg-accent-strong px-4 py-2.5 font-semibold text-white transition hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
        >
          {loading ? "Generating routes…" : "Generate Routes"}
        </button>

        {validationError && (
          <p role="alert" className="rounded-lg border border-amber-800/60 bg-amber-950/40 p-3 text-sm text-amber-200">
            {validationError}
          </p>
        )}
        {error && (
          <p role="alert" className="rounded-lg border border-red-800/60 bg-red-950/40 p-3 text-sm text-red-200">
            {error}
          </p>
        )}

        {result && (
          <div className="space-y-2 border-t border-slate-800 pt-4 text-xs text-slate-400">
            {result.fallbackUsed && (
              <p
                role="status"
                className="rounded-lg border border-amber-800/60 bg-amber-950/40 p-3 text-amber-200"
              >
                Ranking service offline — routes are ordered by distance and
                your preference was not interpreted.
              </p>
            )}
            {!result.fallbackUsed && (
              <p>
                Interpreted as{" "}
                <span className="text-slate-200">
                  {Object.entries(result.parsedPreferences.weights)
                    .map(([k, v]) => `${k} ${(v * 100).toFixed(0)}%`)
                    .join(", ")}
                </span>{" "}
                (confidence {(result.parsedPreferences.confidence * 100).toFixed(0)}%)
              </p>
            )}
            <p>
              Provider: <span className="text-slate-200">{result.provider}</span> · routing{" "}
              {result.routingTimeMs} ms · ranking {result.rankingTimeMs} ms
            </p>
          </div>
        )}
      </div>

      {/* Graph + comparison */}
      <div className="space-y-4">
        <GraphView
          network={network}
          activePath={selectedPath}
          origin={origin}
          destination={destination}
        />

        {!result && !loading && (
          <p className="rounded-xl border border-dashed border-slate-800 p-4 text-sm text-slate-500">
            Describe a preference and generate routes to compare candidates
            side by side. The dashed grey road is closed for construction.
          </p>
        )}

        {result && (
          <div className="space-y-3" aria-label="Route comparison">
            {orderedCandidates.map((candidate) => (
              <RouteCard
                key={candidate.routeId}
                candidate={candidate}
                ranked={rankedById.get(candidate.routeId)}
                isRecommended={candidate.routeId === result.recommendedRouteId}
                isShortest={candidate.routeId === shortestRouteId}
                isSelected={candidate.routeId === selectedRouteId}
                onSelect={setSelectedRouteId}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
