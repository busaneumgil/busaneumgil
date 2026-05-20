import { describe, expect, it } from "vitest";
import source from "./RouteTuningPage.tsx?raw";

describe("RouteTuningPage copy", () => {
  it("separates DB save from manual route apply", () => {
    expect(source).toContain("DB 저장");
    expect(source).toContain("경로 반영");
    expect(source).not.toContain("저장 + 즉시 경로 반영");
    expect(source).not.toContain("즉시 반영");
  });

  it("refreshes the visible draft from the saved segment response", () => {
    expect(source).toContain("setAttributeDraft({");
    expect(source).toContain("normalizeAccessibility(response.segment.walkAccess)");
  });
});
