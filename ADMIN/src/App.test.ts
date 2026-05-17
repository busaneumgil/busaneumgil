import { describe, expect, it } from "vitest";
import appSource from "./App.tsx?raw";

describe("Admin dashboard navigation wiring", () => {
  it("routes dashboard more buttons to the matching admin workspaces", () => {
    expect(appSource).toContain('onOpenRouteStats={() => setPage("routeStats")}');
    expect(appSource).toContain('onOpenBottleneckMonitoring={() => setPage("bottleneckMonitoring")}');
    expect(appSource).toContain('onOpenHazards={() => setPage("hazards")}');
    expect(appSource).toContain('actionTarget="routeStats"');
    expect(appSource).toContain('actionTarget="bottleneckMonitoring"');
    expect(appSource).toContain('actionTarget="hazards"');
  });

  it("wires route statistics and bottleneck monitoring pages to their dedicated admin APIs", () => {
    expect(appSource).toContain("fetchAdminRouteStats");
    expect(appSource).toContain("fetchAdminBottleneckMonitoring");
    expect(appSource).toContain('queryKey: ["admin-route-stats", accessToken]');
    expect(appSource).toContain('queryKey: ["admin-bottleneck-monitoring", accessToken]');
    expect(appSource).toContain('queryFn: () => fetchAdminRouteStats(accessToken)');
    expect(appSource).toContain('queryFn: () => fetchAdminBottleneckMonitoring(accessToken)');
    expect(appSource).toContain('{page === "routeStats" && (');
    expect(appSource).toContain("routeStatsQuery.data");
    expect(appSource).toContain("bottleneckMonitoringQuery.data");
    expect(appSource).toContain("data={usesRealAdminApi ? routeStatsQuery.data : routeStatsMockResponse}");
    expect(appSource).toContain("data={usesRealAdminApi ? bottleneckMonitoringQuery.data : bottleneckMonitoringMockResponse}");
  });
});
