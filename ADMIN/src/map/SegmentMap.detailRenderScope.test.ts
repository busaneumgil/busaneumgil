import { describe, expect, it } from "vitest";
import { detailSegmentRenderScope } from "./SegmentMap";
import type { SegmentFeature } from "../types";

describe("detailSegmentRenderScope", () => {
  it("keeps route-critical segments before ordinary segments when rendering is capped", () => {
    const ordinaryFeatures = Array.from({ length: 1505 }, (_, index) =>
      segmentFeature(index, 129 + index * 0.00001, 35),
    );
    const stairsFeature = segmentFeature("stairs", 129.2, 35.2, { featureTypes: ["STAIRS"] });

    const result = detailSegmentRenderScope([...ordinaryFeatures, stairsFeature], null);

    expect(result.capped).toBe(true);
    expect(result.features).toHaveLength(1500);
    expect(result.features[0].properties.edgeId).toBe("stairs");
  });

  it("keeps segments closest to the current viewport center when ordinary segments exceed the cap", () => {
    const farFeatures = Array.from({ length: 1505 }, (_, index) =>
      segmentFeature(index, 129.007 + index * 0.000001, 35.007),
    );
    const centeredFeature = segmentFeature("centered", 129, 35);

    const result = detailSegmentRenderScope([...farFeatures, centeredFeature], {
      minLng: 128.995,
      minLat: 34.995,
      maxLng: 129.005,
      maxLat: 35.005,
    });

    expect(result.capped).toBe(true);
    expect(result.features.map((feature) => feature.properties.edgeId)).toContain("centered");
  });
});

function segmentFeature(
  edgeId: string | number,
  lng: number,
  lat: number,
  properties: Partial<SegmentFeature["properties"]> = {},
): SegmentFeature {
  return {
    type: "Feature",
    geometry: {
      type: "LineString",
      coordinates: [
        [lng, lat],
        [lng + 0.00005, lat + 0.00005],
      ],
    },
    properties: {
      edgeId,
      segmentType: "SIDE_LINE",
      ...properties,
    },
  };
}
