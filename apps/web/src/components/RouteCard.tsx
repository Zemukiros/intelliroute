"use client";

import type { RankedRoute, RouteCandidate } from "@/lib/types";

interface RouteCardProps {
  candidate: RouteCandidate;
  ranked: RankedRoute | undefined;
  isRecommended: boolean;
  isShortest: boolean;
  isSelected: boolean;
  onSelect: (routeId: string) => void;
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-surface px-2 py-1.5 text-center">
      <div className="text-[10px] uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-0.5 text-sm font-semibold text-slate-100">{value}</div>
    </div>
  );
}

/**
 * One candidate route in the comparison list. Rendered as a button so the
 * whole card is keyboard-focusable and selectable.
 */
export default function RouteCard({
  candidate,
  ranked,
  isRecommended,
  isShortest,
  isSelected,
  onSelect,
}: RouteCardProps) {
  return (
    <button
      type="button"
      onClick={() => onSelect(candidate.routeId)}
      aria-pressed={isSelected}
      aria-label={`Select route ${candidate.path.join(" to ")}`}
      className={`w-full rounded-xl border p-4 text-left transition focus:outline-none focus-visible:ring-2 focus-visible:ring-accent ${
        isSelected
          ? "border-accent bg-accent/5"
          : "border-slate-800 bg-panel hover:border-slate-600"
      }`}
    >
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-mono text-sm text-accent">
          {candidate.path.join(" → ")}
        </span>
        {isRecommended && (
          <span className="rounded-full bg-emerald-500/15 px-2 py-0.5 text-xs font-semibold text-emerald-300">
            ★ Recommended
          </span>
        )}
        {isShortest && (
          <span className="rounded-full bg-sky-500/15 px-2 py-0.5 text-xs font-semibold text-sky-300">
            Shortest
          </span>
        )}
        {ranked && (
          <span className="ml-auto text-xs text-slate-500">
            #{ranked.rank} · score {ranked.score.toFixed(2)}
          </span>
        )}
      </div>

      <div className="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-6">
        <Metric label="Distance" value={`${candidate.totalDistanceKm} km`} />
        <Metric label="Time" value={`${candidate.travelTimeMinutes} min`} />
        <Metric label="Tolls" value={candidate.tollCost > 0 ? `$${candidate.tollCost.toFixed(2)}` : "Free"} />
        <Metric label="Safety" value={`${candidate.safetyScore}/10`} />
        <Metric label="Scenery" value={`${candidate.scenicScore}/10`} />
        <Metric label="Highway" value={`${candidate.highwayPercentage}%`} />
      </div>

      {isRecommended && ranked && (
        <p className="mt-3 border-t border-slate-800 pt-3 text-sm text-slate-300">
          {ranked.explanation}
        </p>
      )}
    </button>
  );
}
