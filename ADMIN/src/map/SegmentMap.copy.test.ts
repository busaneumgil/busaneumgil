import { describe, expect, it } from "vitest";
import source from "./SegmentMap.tsx?raw";

describe("SegmentMap route review guardrail", () => {
  it("caps forced detailed rendering when too many segments are loaded", () => {
    expect(source).toContain("FORCED_DETAIL_SEGMENT_MAX_COUNT");
    expect(source).toContain("상세 렌더링을 제한했습니다");
  });
});
