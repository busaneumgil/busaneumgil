import { describe, expect, it } from "vitest";
import source from "./HazardRouteReviewWorkspace.tsx?raw";

describe("HazardRouteReviewWorkspace copy", () => {
  it("keeps completion copy aligned with db sync flow", () => {
    expect(source).toContain("검수 완료");
    expect(source).toContain("전체 DB 반영 대기 목록에 포함됩니다");
  });
});
