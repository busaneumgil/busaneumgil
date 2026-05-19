import { describe, expect, it } from "vitest";
import source from "./HazardRouteReviewWorkspace.tsx?raw";

describe("HazardRouteReviewWorkspace copy", () => {
  it("makes the completion action describe immediate route apply", () => {
    expect(source).toContain("검수 완료 및 즉시 반영");
    expect(source).toContain("사용자 재탐색부터 경로 계산에 반영됩니다");
  });
});
