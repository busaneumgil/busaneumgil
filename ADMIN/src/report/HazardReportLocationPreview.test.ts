import { describe, expect, it } from "vitest";
import source from "./HazardReportLocationPreview.tsx?raw";

describe("HazardReportLocationPreview map chrome", () => {
  it("does not cover the map with the old roadview instruction hint", () => {
    expect(source).not.toContain("hazard-location-map__hint");
    expect(source).not.toContain("지도를 드래그한 뒤 원하는 지점을 클릭하면 해당 위치 기준 로드뷰를 확인할 수 있습니다.");
  });
});
