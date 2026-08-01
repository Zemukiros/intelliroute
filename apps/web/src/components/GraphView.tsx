"use client";

import type { Network } from "@/lib/types";

interface GraphViewProps {
  network: Network;
  /** Node ids on the active route, in order; empty when no route shown. */
  activePath: string[];
  origin: string | null;
  destination: string | null;
}

/**
 * Lightweight SVG rendering of the road network.
 *
 * Uses the abstract layout coordinates served by the API; edges on the
 * active route are highlighted. No external map or chart library needed.
 */
export default function GraphView({
  network,
  activePath,
  origin,
  destination,
}: GraphViewProps) {
  const nodeById = new Map(network.nodes.map((n) => [n.id, n]));

  const pathEdges = new Set<string>();
  for (let i = 0; i < activePath.length - 1; i++) {
    const [a, b] = [activePath[i], activePath[i + 1]].sort();
    pathEdges.add(`${a}-${b}`);
  }
  const pathNodes = new Set(activePath);

  const edgeKey = (from: string, to: string) =>
    [from, to].sort().join("-");

  return (
    <svg
      viewBox="0 0 760 520"
      role="img"
      aria-label="Road network graph"
      className="h-auto w-full rounded-xl border border-slate-800 bg-panel"
    >
      {/* Edges */}
      {network.edges.map((edge) => {
        const from = nodeById.get(edge.from);
        const to = nodeById.get(edge.to);
        if (!from || !to) return null;
        const onPath = pathEdges.has(edgeKey(edge.from, edge.to));
        const midX = (from.x + to.x) / 2;
        const midY = (from.y + to.y) / 2;
        return (
          <g key={`${edge.from}-${edge.to}`}>
            <line
              x1={from.x}
              y1={from.y}
              x2={to.x}
              y2={to.y}
              stroke={onPath ? "#38bdf8" : "#334155"}
              strokeWidth={onPath ? 3.5 : 1.5}
              strokeLinecap="round"
            />
            <text
              x={midX}
              y={midY - 6}
              textAnchor="middle"
              fontSize="11"
              fill={onPath ? "#7dd3fc" : "#64748b"}
            >
              {edge.distanceKm} km
            </text>
          </g>
        );
      })}

      {/* Nodes */}
      {network.nodes.map((node) => {
        const isOrigin = node.id === origin;
        const isDestination = node.id === destination;
        const onPath = pathNodes.has(node.id);
        const fill = isOrigin
          ? "#22c55e"
          : isDestination
            ? "#f59e0b"
            : onPath
              ? "#38bdf8"
              : "#1e293b";
        const stroke = onPath || isOrigin || isDestination ? "#e2e8f0" : "#475569";
        return (
          <g key={node.id}>
            <circle cx={node.x} cy={node.y} r={16} fill={fill} stroke={stroke} strokeWidth={1.5} />
            <text
              x={node.x}
              y={node.y + 4}
              textAnchor="middle"
              fontSize="12"
              fontWeight="700"
              fill="#f8fafc"
            >
              {node.id}
            </text>
            <text
              x={node.x}
              y={node.y + 32}
              textAnchor="middle"
              fontSize="11"
              fill="#94a3b8"
            >
              {node.name}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
