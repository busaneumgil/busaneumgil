import type { EditAction, SegmentFeature } from "../types";
import type { Coord } from "./routeGraph";

type AddSegmentType = Extract<EditAction, { action: "add_segment" }>["segmentType"];

export interface CoordBounds {
  minLng: number;
  minLat: number;
  maxLng: number;
  maxLat: number;
}

export function deletedEdgeIds(edits: EditAction[]): Set<string> {
  return new Set(
    edits
      .filter((edit): edit is Extract<EditAction, { action: "delete_segment" }> => edit.action === "delete_segment")
      .map((edit) => String(edit.edgeId)),
  );
}

export function visibleSegmentFeatures(segments: SegmentFeature[], edits: EditAction[]): SegmentFeature[] {
  const deleted = deletedEdgeIds(edits);
  return segments.filter((feature) => !deleted.has(String(feature.properties.edgeId)));
}

export function draftSegmentFeatures(edits: EditAction[]): SegmentFeature[] {
  return edits.flatMap((edit, index) => {
    if (edit.action !== "add_segment") return [];
    return [
      {
        type: "Feature",
        geometry: edit.geom,
        properties: {
          edgeId: `draft:${index}`,
          segmentType: edit.segmentType,
        },
      } satisfies SegmentFeature,
    ];
  });
}

export function twoPointAddDraft(segmentType: AddSegmentType, points: Coord[]): {
  edit: Extract<EditAction, { action: "add_segment" }> | null;
  remainingPoints: Coord[];
} {
  if (points.length < 2) return { edit: null, remainingPoints: points };
  return {
    edit: {
      action: "add_segment",
      segmentType,
      geom: { type: "LineString", coordinates: points.slice(0, 2) },
    },
    remainingPoints: [],
  };
}

export function resetPolygonDeleteSelection(active: boolean): { active: boolean; points: Coord[]; pointCount: number } {
  return { active, points: [], pointCount: 0 };
}

export function coordBounds(a: Coord, b: Coord): CoordBounds {
  return {
    minLng: Math.min(a[0], b[0]),
    minLat: Math.min(a[1], b[1]),
    maxLng: Math.max(a[0], b[0]),
    maxLat: Math.max(a[1], b[1]),
  };
}

export function segmentsIntersectingBounds(segments: SegmentFeature[], bounds: CoordBounds): SegmentFeature[] {
  return segments.filter((feature) => lineIntersectsBounds(feature.geometry.coordinates, bounds));
}

export function segmentsTouchingPolygon(segments: SegmentFeature[], polygon: Coord[]): SegmentFeature[] {
  if (polygon.length < 3) return [];
  const bounds = coordBoundsForCoords(polygon);
  return segments.filter((feature) => boundsIntersect(featureCoordBounds(feature.geometry.coordinates), bounds))
    .filter((feature) => feature.geometry.coordinates.some((coord) => pointInPolygon(coord, polygon)));
}

function boundsIntersect(a: CoordBounds, b: CoordBounds): boolean {
  return a.minLng <= b.maxLng && a.maxLng >= b.minLng && a.minLat <= b.maxLat && a.maxLat >= b.minLat;
}

function lineIntersectsBounds(coordinates: Coord[], bounds: CoordBounds): boolean {
  if (coordinates.some((coord) => pointInBounds(coord, bounds))) return true;
  if (!boundsIntersect(featureCoordBounds(coordinates), bounds)) return false;

  return coordinates.slice(1).some((coord, index) => segmentIntersectsBounds(coordinates[index], coord, bounds));
}

function featureCoordBounds(coordinates: Coord[]): CoordBounds {
  return coordinates.reduce<CoordBounds>(
    (acc, [lng, lat]) => ({
      minLng: Math.min(acc.minLng, lng),
      minLat: Math.min(acc.minLat, lat),
      maxLng: Math.max(acc.maxLng, lng),
      maxLat: Math.max(acc.maxLat, lat),
    }),
    { minLng: Infinity, minLat: Infinity, maxLng: -Infinity, maxLat: -Infinity },
  );
}

function coordBoundsForCoords(coordinates: Coord[]): CoordBounds {
  return coordinates.reduce<CoordBounds>(
    (acc, [lng, lat]) => ({
      minLng: Math.min(acc.minLng, lng),
      minLat: Math.min(acc.minLat, lat),
      maxLng: Math.max(acc.maxLng, lng),
      maxLat: Math.max(acc.maxLat, lat),
    }),
    { minLng: Infinity, minLat: Infinity, maxLng: -Infinity, maxLat: -Infinity },
  );
}

function pointInPolygon(coord: Coord, polygon: Coord[]): boolean {
  const [x, y] = coord;
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const [xi, yi] = polygon[i];
    const [xj, yj] = polygon[j];
    if ((yi > y) !== (yj > y) && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
}

function pointInBounds([lng, lat]: Coord, bounds: CoordBounds): boolean {
  return lng >= bounds.minLng && lng <= bounds.maxLng && lat >= bounds.minLat && lat <= bounds.maxLat;
}

function segmentIntersectsBounds(a: Coord, b: Coord, bounds: CoordBounds): boolean {
  const bottomLeft: Coord = [bounds.minLng, bounds.minLat];
  const bottomRight: Coord = [bounds.maxLng, bounds.minLat];
  const topRight: Coord = [bounds.maxLng, bounds.maxLat];
  const topLeft: Coord = [bounds.minLng, bounds.maxLat];

  return (
    segmentsIntersect(a, b, bottomLeft, bottomRight) ||
    segmentsIntersect(a, b, bottomRight, topRight) ||
    segmentsIntersect(a, b, topRight, topLeft) ||
    segmentsIntersect(a, b, topLeft, bottomLeft)
  );
}

function segmentsIntersect(a: Coord, b: Coord, c: Coord, d: Coord): boolean {
  const abC = direction(a, b, c);
  const abD = direction(a, b, d);
  const cdA = direction(c, d, a);
  const cdB = direction(c, d, b);

  if (abC === 0 && pointOnSegment(a, b, c)) return true;
  if (abD === 0 && pointOnSegment(a, b, d)) return true;
  if (cdA === 0 && pointOnSegment(c, d, a)) return true;
  if (cdB === 0 && pointOnSegment(c, d, b)) return true;

  return abC !== abD && cdA !== cdB;
}

function direction(a: Coord, b: Coord, c: Coord): -1 | 0 | 1 {
  const cross = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
  if (Math.abs(cross) < 1e-12) return 0;
  return cross > 0 ? 1 : -1;
}

function pointOnSegment(a: Coord, b: Coord, c: Coord): boolean {
  return (
    c[0] >= Math.min(a[0], b[0]) &&
    c[0] <= Math.max(a[0], b[0]) &&
    c[1] >= Math.min(a[1], b[1]) &&
    c[1] <= Math.max(a[1], b[1])
  );
}
