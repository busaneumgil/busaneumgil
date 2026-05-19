import { describe, expect, it } from "vitest";
import source from "./HazardReportsPage.tsx?raw";

describe("HazardReportsPage manual routing apply wiring", () => {
  it("connects the bulk apply button and refreshes apply-state after review completion", () => {
    expect(source).toContain("onClick={() => applyRoutingMutation.mutate()}");
    expect(source).toContain('invalidateQueries({ queryKey: ["admin-routing-apply-state"] })');
    expect(source).toContain('meta: "(경로 반영 대기)"');
    expect(source).not.toContain("(백엔드 연동 예정)");
  });
});
