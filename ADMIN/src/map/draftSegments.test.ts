import { describe, expect, it } from "vitest";
import type { EditAction, SegmentFeature } from "../types";
import { coordBounds, draftSegmentFeatures, resetPolygonDeleteSelection, segmentsIntersectingBounds, segmentsTouchingPolygon, twoPointAddDraft, visibleSegmentFeatures } from "./draftSegments";

function segment(edgeId: string, coordinates: Array<[number, number]>): SegmentFeature {
  return {
    type: "Feature",
    geometry: { type: "LineString", coordinates },
    properties: { edgeId, segmentType: "SIDE_LINE" },
  };
}

describe("draft segment helpers", () => {
  it("hides deleted existing segments and materializes added draft segments", () => {
    const edits: EditAction[] = [
      { action: "delete_segment", edgeId: "2" },
      {
        action: "add_segment",
        segmentType: "CROSS_WALK",
        geom: { type: "LineString", coordinates: [[129, 35], [129.001, 35]] },
      },
    ];

    expect(visibleSegmentFeatures([segment("1", []), segment("2", [])], edits).map((item) => item.properties.edgeId)).toEqual(["1"]);
    expect(draftSegmentFeatures(edits)).toMatchObject([
      { properties: { edgeId: "draft:1", segmentType: "CROSS_WALK" } },
    ]);
  });

  it("selects segments whose coordinate bounds intersect a drag box", () => {
    const matches = segmentsIntersectingBounds(
      [
        segment("inside", [[129, 35], [129.001, 35.001]]),
        segment("outside", [[130, 36], [130.001, 36.001]]),
      ],
      coordBounds([128.999, 34.999], [129.002, 35.002]),
    );

    expect(matches.map((item) => item.properties.edgeId)).toEqual(["inside"]);
  });

  it("does not select an L-shaped segment whose bbox intersects but line does not", () => {
    const matches = segmentsIntersectingBounds(
      [segment("around", [[129, 35], [129, 35.01], [129.01, 35.01]])],
      coordBounds([129.004, 35.004], [129.006, 35.006]),
    );

    expect(matches).toEqual([]);
  });

  it("matches the existing editor polygon delete rule", () => {
    const matches = segmentsTouchingPolygon(
      [
        segment("inside", [[129.005, 35.005], [129.006, 35.006]]),
        segment("outside", [[129.02, 35.02], [129.03, 35.03]]),
      ],
      [[129, 35], [129.01, 35], [129.01, 35.01], [129, 35.01]],
    );

    expect(matches.map((item) => item.properties.edgeId)).toEqual(["inside"]);
  });

  it("finishes one add draft after exactly two points", () => {
    const result = twoPointAddDraft("CROSS_WALK", [[129, 35], [129.001, 35]]);

    expect(result.edit).toMatchObject({
      action: "add_segment",
      segmentType: "CROSS_WALK",
      geom: { coordinates: [[129, 35], [129.001, 35]] },
    });
    expect(result.remainingPoints).toEqual([]);
  });

  it("keeps polygon delete mode active after clearing a completed polygon", () => {
    expect(resetPolygonDeleteSelection(true)).toEqual({ active: true, points: [], pointCount: 0 });
  });
});
