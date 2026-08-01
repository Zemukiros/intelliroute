"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError, calculateRoute, fetchNetwork } from "@/lib/api";
import type { Network, RouteResult } from "@/lib/types";
import GraphView from "./GraphView";

type LoadState = "loading" | "ready" | "offline";

/**
 * Interactive route planner: pick an origin and destination, calculate the
 * shortest route via the IntelliRoute API, and inspect the result.
 */
export default function RoutePlanner() {
  const [network, setNetwork] = useState<Network | null>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [origin, setOrigin] = useState("A");
  const [destination, setDestination] = useState("F");
  const [result, setResult] = useState<RouteResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [calculating, setCalculating] = useState(false);

  const loadNetwork = useCallback(async () => {
    setLoadState("loading");
    try {
      const data = await fetchNetwork();
      setNetwork(data);
      setLoadState("ready");
    } catch {
      setLoadState("offline");
    }
  }, []);

  useEffect(() => {
    void loadNetwork();
  }, [loadNetwork]);

  const onCalculate = async () => {
    setCalculating(true);
    setError(null);
    setResult(null);
    try {
      setResult(await calculateRoute(origin, destination));
    } catch (e) {
      if (e instanceof ApiError && e.problem) {
        setError(`${e.problem.error}: ${e.problem.message}`);
      } else {
        setError("Could not reach the IntelliRoute API. Is the backend running on port 8080?");
      }
    } finally {
      setCalculating(false);
    }
  };

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
          <code className="rounded bg-black/40 px-1.5 py-0.5">
            mvn spring-boot:run
          </code>{" "}
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
    <div className="grid gap-6 lg:grid-cols-[340px_1fr]">
      {/* Control panel */}
      <div className="space-y-5 rounded-xl border border-slate-800 bg-panel p-6">
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

        <button
          onClick={() => void onCalculate()}
          disabled={calculating}
          className="w-full rounded-lg bg-accent-strong px-4 py-2.5 font-semibold text-white transition hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
        >
          {calculating ? "Calculating…" : "Calculate Route"}
        </button>

        {error && (
          <p role="alert" className="rounded-lg border border-red-800/60 bg-red-950/40 p-3 text-sm text-red-200">
            {error}
          </p>
        )}

        {result && (
          <dl className="space-y-3 border-t border-slate-800 pt-4 text-sm">
            <div>
              <dt className="text-slate-400">Shortest path</dt>
              <dd className="mt-1 font-mono text-base text-accent">
                {result.path.join(" → ")}
              </dd>
            </div>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="rounded-lg bg-surface p-3">
                <dt className="text-xs text-slate-400">Distance</dt>
                <dd className="mt-1 font-semibold text-slate-100">
                  {result.totalDistance} km
                </dd>
              </div>
              <div className="rounded-lg bg-surface p-3">
                <dt className="text-xs text-slate-400">Visited</dt>
                <dd className="mt-1 font-semibold text-slate-100">
                  {result.visitedNodes} nodes
                </dd>
              </div>
              <div className="rounded-lg bg-surface p-3">
                <dt className="text-xs text-slate-400">Engine time</dt>
                <dd className="mt-1 font-semibold text-slate-100">
                  {result.executionTimeMs} ms
                </dd>
              </div>
            </div>
          </dl>
        )}
      </div>

      {/* Graph visualization */}
      <div>
        <GraphView
          network={network}
          activePath={result?.path ?? []}
          origin={origin}
          destination={destination}
        />
        <p className="mt-3 text-xs text-slate-500">
          Sample road network — distances in kilometres. Juniper Isle (J) is
          intentionally disconnected to demonstrate unreachable-route handling.
        </p>
      </div>
    </div>
  );
}
