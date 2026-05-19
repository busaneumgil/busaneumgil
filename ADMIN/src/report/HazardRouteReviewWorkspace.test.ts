import { describe, expect, it } from "vitest";
import source from "./HazardRouteReviewWorkspace.tsx?raw";

describe("HazardRouteReviewWorkspace copy", () => {
  it("removes immediate route apply copy from the completion action", () => {
    expect(source).toContain("검수 완료");
    expect(source).toContain("DB 저장 후 경로 반영 버튼");
    expect(source).not.toContain("즉시 반영");
  });
});
