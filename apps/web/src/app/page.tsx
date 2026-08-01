import RoutePlanner from "@/components/RoutePlanner";

export default function HomePage() {
  return (
    <main className="mx-auto max-w-6xl px-6 py-10">
      <header className="mb-8">
        <p className="text-sm font-semibold uppercase tracking-widest text-accent">
          IntelliRoute
        </p>
        <h1 className="mt-2 text-3xl font-bold text-slate-50">
          Route Intelligence Platform
        </h1>
        <p className="mt-3 max-w-2xl text-slate-400">
          Shortest-path routing over a weighted road network using
          Dijkstra&apos;s algorithm, served by a Spring Boot API with measured
          execution time. AI-assisted route preferences arrive in a later
          milestone.
        </p>
      </header>
      <RoutePlanner />
    </main>
  );
}
