import { describe, expect, it } from "vitest";
import source from "./SegmentMap.tsx?raw";

describe("SegmentMap route review guardrail", () => {
  it("caps forced detailed rendering when too many segments are loaded", () => {
    expect(source).toContain("FORCED_DETAIL_SEGMENT_MAX_COUNT");
    expect(source).toContain("상세 렌더링을 제한했습니다");
  });

  it("caps zoom-triggered overlay rendering to protect the browser main thread", () => {
    expect(source).toContain("DETAIL_SEGMENT_RENDER_MAX_COUNT");
    expect(source).toContain("detailSegmentRenderScope");
    expect(source).toContain("화면 보호를 위해 일부만 표시 중");
  });

  it("uses a high-contrast overlay for the selected segment", () => {
    expect(source).toContain('strokeColor: "#f97316"');
    expect(source).toContain("zIndex: 80");
  });
});
