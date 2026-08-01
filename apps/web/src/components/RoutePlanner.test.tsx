import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import RoutePlanner from "./RoutePlanner";
import { errorJson, network, okJson, recommendResult } from "@/test/fixtures";

function mockFetchSequence(...responses: Response[]) {
  const fn = vi.fn();
  responses.forEach((r) => fn.mockResolvedValueOnce(r));
  vi.stubGlobal("fetch", fn);
  return fn;
}

async function renderReady() {
  mockFetchSequence(okJson(network));
  render(<RoutePlanner />);
  await screen.findByLabelText("Origin");
}

describe("RoutePlanner", () => {
  it("shows the offline recovery card when the network fetch fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("down")));
    render(<RoutePlanner />);

    expect(await screen.findByText("API not reachable")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /retry connection/i })).toBeInTheDocument();
  });

  it("submits a preference and renders ranked route cards", async () => {
    const fetchMock = mockFetchSequence(okJson(network), okJson(recommendResult));
    render(<RoutePlanner />);
    await screen.findByLabelText("Origin");

    await userEvent.clear(screen.getByLabelText(/route preference/i));
    await userEvent.type(screen.getByLabelText(/route preference/i), "avoid tolls");
    await userEvent.click(screen.getByRole("button", { name: /generate routes/i }));

    // Recommended badge and explanation from the ranking service
    expect(await screen.findByText("★ Recommended")).toBeInTheDocument();
    expect(screen.getByText(/Best match: lowest toll cost/)).toBeInTheDocument();
    // Both candidates rendered
    expect(screen.getByText("A → C → E → F")).toBeInTheDocument();
    expect(screen.getByText("A → C → D → F")).toBeInTheDocument();
    // Shortest badge on the distance-minimal route
    expect(screen.getByText("Shortest")).toBeInTheDocument();
    // Recommend endpoint was called with the typed preference
    const recommendCall = fetchMock.mock.calls[1];
    expect(recommendCall[0]).toContain("/api/routes/recommend");
    expect(JSON.parse(recommendCall[1].body as string).preference).toBe("avoid tolls");
  });

  it("shows the parsed-preference summary after ranking", async () => {
    mockFetchSequence(okJson(network), okJson(recommendResult));
    render(<RoutePlanner />);
    await screen.findByLabelText("Origin");

    await userEvent.click(screen.getByRole("button", { name: /generate routes/i }));

    expect(await screen.findByText(/toll 100%/)).toBeInTheDocument();
    expect(screen.getByText(/local-deterministic-v1/)).toBeInTheDocument();
  });

  it("selecting a card highlights that route on the graph", async () => {
    mockFetchSequence(okJson(network), okJson(recommendResult));
    render(<RoutePlanner />);
    await screen.findByLabelText("Origin");
    await userEvent.click(screen.getByRole("button", { name: /generate routes/i }));
    await screen.findByText("★ Recommended");

    // Recommended (route-3: A,C,E,F) is selected initially — its A-C edge is highlighted.
    const edgeAC = screen.getByTestId("edge-A-C").querySelector("line");
    expect(edgeAC).toHaveAttribute("stroke", "#38bdf8");
    // D-F belongs only to route-1 and is not highlighted yet.
    expect(screen.getByTestId("edge-D-F").querySelector("line"))
      .toHaveAttribute("stroke", "#334155");

    // Select the other candidate.
    await userEvent.click(screen.getByRole("button", { name: /select route A to C to D to F/i }));

    expect(screen.getByTestId("edge-D-F").querySelector("line"))
      .toHaveAttribute("stroke", "#38bdf8");
  });

  it("renders closed roads dashed and never highlights them", async () => {
    mockFetchSequence(okJson(network), okJson(recommendResult));
    render(<RoutePlanner />);
    await screen.findByLabelText("Origin");

    const closed = screen.getByTestId("edge-B-E").querySelector("line");
    expect(closed).toHaveAttribute("stroke-dasharray", "6 5");
    expect(screen.getByText("closed")).toBeInTheDocument();
  });

  it("blocks submission with a validation message when the preference is blank", async () => {
    mockFetchSequence(okJson(network));
    render(<RoutePlanner />);
    await screen.findByLabelText("Origin");

    await userEvent.clear(screen.getByLabelText(/route preference/i));
    await userEvent.click(screen.getByRole("button", { name: /generate routes/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/describe a route preference/i);
  });

  it("shows the ranking-service-offline fallback notice", async () => {
    mockFetchSequence(
      okJson(network),
      okJson({
        ...recommendResult,
        fallbackUsed: true,
        provider: "java-local-fallback",
      }),
    );
    render(<RoutePlanner />);
    await screen.findByLabelText("Origin");
    await userEvent.click(screen.getByRole("button", { name: /generate routes/i }));

    expect(await screen.findByRole("status")).toHaveTextContent(/ranking service offline/i);
  });

  it("renders structured API errors (unreachable destination)", async () => {
    mockFetchSequence(
      okJson(network),
      errorJson(422, {
        timestamp: "t",
        status: 422,
        error: "ROUTE_UNREACHABLE",
        message: "No route exists from 'A' to 'J'",
      }),
    );
    render(<RoutePlanner />);
    await screen.findByLabelText("Origin");
    await userEvent.click(screen.getByRole("button", { name: /generate routes/i }));

    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(/ROUTE_UNREACHABLE/));
  });

  it("clicking an example chip fills the preference input", async () => {
    await renderReady();

    await userEvent.click(screen.getByRole("button", { name: "Avoid highways" }));

    expect(screen.getByLabelText(/route preference/i)).toHaveValue("Avoid highways");
  });
});
