import { describe, expect, it } from "vitest";
import source from "./HazardReportsPage.tsx?raw";

describe("HazardReportsPage manual routing apply wiring", () => {
  it("connects the bulk apply button and refreshes apply-state after review completion", () => {
    expect(source).toContain("onClick={() => applyRoutingMutation.mutate()}");
    expect(source).toContain('invalidateQueries({ queryKey: ["admin-routing-apply-state"] })');
    expect(source).toContain('invalidateQueries({ queryKey: ["admin-hazard-route-review-network"] })');
    expect(source).toContain('meta: "(경로 반영 대기)"');
    expect(source).not.toContain("(諛깆뿏???곕룞 ?덉젙)");
  });

  it("loads route-review network by selected gu/dong without radius or limit clipping", () => {
    expect(source).not.toContain("const HAZARD_ROUTE_REVIEW_RADIUS_METER = 150");
    expect(source).not.toContain("const HAZARD_ROUTE_REVIEW_SEGMENT_LIMIT = 500");
    expect(source).not.toContain("centerLat:");
    expect(source).not.toContain("centerLng:");
    expect(source).not.toContain("radiusMeter:");
    expect(source).toContain('&& detailPaneMode === "review"');
  });

  it("keeps the reject action available during approve review progress", () => {
    expect(source).toContain("canRejectHazardReport(detail.status, activeReviewDraft)");
    expect(source).toContain("clearRouteReviewDraft(response.reportId)");
    expect(source).not.toContain('disabled={!canReject || activeReviewDraft?.stage === "IN_PROGRESS"}');
  });
});
