import { type RefObject, useEffect, useRef, useState } from "react";
import type { BridgeFeature, BridgePayload, EditableSegmentType, EditAction, GeoPoint, ReferenceLayerKey, ReferencePointFeature, ReferencePointPayload, RoadAttributeFeature, RoadAttributePayload, SegmentFeature, SegmentFeatureType, SegmentPayload } from "../types";
import { attachKakaoWheelZoom, loadKakaoMap, type KakaoMap, type KakaoOverlay, type KakaoRoadview, type KakaoRoadviewClient } from "./kakaoLoader";
import { deletedEdgeIds, draftSegmentFeatures, resetPolygonDeleteSelection, segmentsTouchingPolygon, twoPointAddDraft, visibleSegmentFeatures } from "./draftSegments";
import { shouldShowRoadAttributeReference } from "./networkReferenceLayer";
import { roadAttributeStrokeColor, roadAttributeStrokeStyle, roadAttributeStrokeWeight } from "./roadAttributeStyle";
import { roadviewUnavailableMessage, shouldOpenRoadviewForMode } from "./roadviewMode";
import { pointLineDistanceM } from "./referenceMatching";

type Coord = [number, number];
type EditorMode = "idle" | "delete" | "add" | "roadview";
type AddType = EditableSegmentType;

interface SegmentMapProps {
  payload?: SegmentPayload;
  bridgePayload?: BridgePayload;
  loading: boolean;
  error?: Error | null;
  draftEdits: EditAction[];
  onDraftEdit: (edit: EditAction) => void;
  selectedSegment: SegmentFeature | null;
  onSelectSegment: (feature: SegmentFeature) => void;
  referenceLayers?: Record<ReferenceLayerKey, boolean>;
  roadAttributePayload?: RoadAttributePayload;
  stairPayload?: ReferencePointPayload;
  audioSignalPayload?: ReferencePointPayload;
  brailleBlockPayload?: ReferencePointPayload;
  roadviewContainerRef: RefObject<HTMLDivElement | null>;
  onRoadviewChange: (state: RoadviewDockState) => void;
  editable?: boolean;
  routePointPickMode?: "start" | "end" | null;
  onRoutePointPick?: (point: GeoPoint) => void;
  routeLines?: {
    safe?: GeoPoint[];
    fast?: GeoPoint[];
  };
  routePoints?: {
    start?: GeoPoint | null;
    end?: GeoPoint | null;
  };
  toolbarMode?: "editor" | "roadSegmentLegend" | "segmentFeatureLegend";
  draftEditCount?: number;
  onUndoDraftEdit?: () => void;
  onClearDraftEdits?: () => void;
}

export interface RoadviewDockState {
  open: boolean;
  message: string;
  onClose?: () => void;
}

const ROADVIEW_DEFAULT_MESSAGE = "Roadview 도구를 누른 뒤 지도를 클릭하면 Kakao Roadview를 엽니다.";
const DETAIL_SEGMENT_MAX_LEVEL = 4;
const segmentFeatureTypes: SegmentFeatureType[] = ["CROSSWALK", "AUDIO_SIGNAL", "BRAILLE_BLOCK", "STAIRS"];
const segmentFeatureLabels: Record<SegmentFeatureType, string> = {
  CROSSWALK: "횡단보도",
  AUDIO_SIGNAL: "음향신호기",
  BRAILLE_BLOCK: "점자블록",
  STAIRS: "계단",
};
const segmentFeatureColors: Record<SegmentFeatureType, string> = {
  CROSSWALK: "#2563eb",
  AUDIO_SIGNAL: "#0f766e",
  BRAILLE_BLOCK: "#7c3aed",
  STAIRS: "#7c2d12",
};

export function SegmentMap({
  payload,
  bridgePayload,
  loading,
  error,
  draftEdits,
  onDraftEdit,
  selectedSegment,
  onSelectSegment,
  referenceLayers,
  roadAttributePayload,
  stairPayload,
  audioSignalPayload,
  brailleBlockPayload,
  roadviewContainerRef,
  onRoadviewChange,
  editable = true,
  routePointPickMode = null,
  onRoutePointPick,
  routeLines,
  routePoints,
  toolbarMode = "editor",
  draftEditCount = 0,
  onUndoDraftEdit,
  onClearDraftEdits,
}: SegmentMapProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const detachWheelZoomRef = useRef<(() => void) | null>(null);
  const roadviewRef = useRef<KakaoRoadview | null>(null);
  const roadviewClientRef = useRef<KakaoRoadviewClient | null>(null);
  const roadviewMarkerRef = useRef<KakaoOverlay | null>(null);
  const roadviewArrowElRef = useRef<HTMLDivElement | null>(null);
  const overlaysRef = useRef<KakaoOverlay[]>([]);
  const tempOverlaysRef = useRef<KakaoOverlay[]>([]);
  const pendingEditOverlaysRef = useRef<KakaoOverlay[]>([]);
  const referenceOverlaysRef = useRef<KakaoOverlay[]>([]);
  const routeOverlaysRef = useRef<KakaoOverlay[]>([]);
  const routePointOverlaysRef = useRef<KakaoOverlay[]>([]);
  const segmentFeatureOverlaysRef = useRef<KakaoOverlay[]>([]);
  const selectedSegmentOverlayRef = useRef<KakaoOverlay | null>(null);
  const roadAttributeTooltipRef = useRef<KakaoOverlay | null>(null);
  const segmentOverlayByEdgeRef = useRef<Map<string, KakaoOverlay[]>>(new Map());
  const polygonShapeRef = useRef<KakaoOverlay | null>(null);
  const centeredPayloadRef = useRef<{ payload?: SegmentPayload; bridgePayload?: BridgePayload }>({});
  const draftEditsRef = useRef<EditAction[]>(draftEdits);
  const modeRef = useRef<EditorMode>("idle");
  const addTypeRef = useRef<AddType>("SIDE_LINE");
  const addPointsRef = useRef<Array<[number, number]>>([]);
  const polygonPointsRef = useRef<Coord[]>([]);
  const polygonDeleteActiveRef = useRef(false);
  const onDraftEditRef = useRef(onDraftEdit);
  const onSelectSegmentRef = useRef(onSelectSegment);
  const routePointPickModeRef = useRef(routePointPickMode);
  const onRoutePointPickRef = useRef(onRoutePointPick);
  const [mode, setModeState] = useState<EditorMode>("idle");
  const [addType, setAddTypeState] = useState<AddType>("SIDE_LINE");
  const [pendingAddCount, setPendingAddCount] = useState(0);
  const [mapError, setMapError] = useState<string | null>(null);
  const [mapReady, setMapReady] = useState(false);
  const [mapLevel, setMapLevel] = useState(6);
  const [snapMessage, setSnapMessage] = useState<string | null>(null);
  const [polygonDeleteActive, setPolygonDeleteActive] = useState(false);
  const [polygonPointCount, setPolygonPointCount] = useState(0);
  const [roadSegmentLayers, setRoadSegmentLayers] = useState({
    sideLine: true,
    crossWalk: true,
    transitionConnector: true,
  });
  const [segmentFeatureLayers, setSegmentFeatureLayers] = useState<Record<SegmentFeatureType, boolean>>({
    CROSSWALK: true,
    AUDIO_SIGNAL: true,
    BRAILLE_BLOCK: true,
    STAIRS: true,
  });
  const detailedSegmentsVisible = mapLevel <= DETAIL_SEGMENT_MAX_LEVEL;

  useEffect(() => {
    onDraftEditRef.current = onDraftEdit;
    onSelectSegmentRef.current = onSelectSegment;
    draftEditsRef.current = draftEdits;
    routePointPickModeRef.current = routePointPickMode;
    onRoutePointPickRef.current = onRoutePointPick;
  }, [draftEdits, onDraftEdit, onRoutePointPick, onSelectSegment, routePointPickMode]);

  useEffect(() => {
    if (!editable && (modeRef.current === "add" || modeRef.current === "delete")) {
      setMode("idle");
    }
  }, [editable]);

  useEffect(() => {
    let disposed = false;

    loadKakaoMap()
      .then(() => {
        if (disposed || !containerRef.current || mapRef.current || !window.kakao?.maps) return;
        const center = new window.kakao.maps.LatLng(35.1796, 129.0756);
        mapRef.current = new window.kakao.maps.Map(containerRef.current, {
          center,
          level: 6,
        });
        setMapLevel(mapRef.current.getLevel?.() ?? 6);
        detachWheelZoomRef.current?.();
        detachWheelZoomRef.current = attachKakaoWheelZoom(containerRef.current, () => mapRef.current, setMapLevel);
        roadviewClientRef.current = window.kakao.maps.RoadviewClient ? new window.kakao.maps.RoadviewClient() : null;
        setMapReady(true);
        window.kakao.maps.event.addListener(mapRef.current, "click", (event: unknown) => {
          const latLng = (event as { latLng?: { getLng: () => number; getLat: () => number } }).latLng;
          if (!latLng) return;
          const coord: [number, number] = [latLng.getLng(), latLng.getLat()];
          handleMapCoordinate(coord, latLng);
        });
        window.kakao.maps.event.addListener(mapRef.current, "zoom_changed", () => {
          setMapLevel(mapRef.current?.getLevel?.() ?? 6);
        });
      })
      .catch((reason: Error) => setMapError(reason.message));

    return () => {
      disposed = true;
      detachWheelZoomRef.current?.();
      detachWheelZoomRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapReady || !mapRef.current || !window.kakao?.maps) return;

    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = [];
    segmentOverlayByEdgeRef.current.clear();

    const useHitArea = toolbarMode === "editor" && mode === "delete";
    const canRenderDetails = detailedSegmentsVisible;
    const allSegmentFeatures = visibleSegmentFeatures(payload?.segments.features ?? [], draftEditsRef.current);
    const segmentFeatures = canRenderDetails
      ? allSegmentFeatures.filter(shouldShowRoadSegmentLayer)
      : [];
    segmentFeatures.forEach((feature) => {
      const segmentOverlays = createSegmentOverlay(feature, mapRef.current!, (coord, latLng) => {
        if (routePointPickModeRef.current) {
          handleMapCoordinate(coord, latLng);
          return;
        }
        if (toolbarMode !== "editor") {
          onSelectSegmentRef.current(feature);
          drawSelectedSegment(feature);
          return;
        }
        if (modeRef.current === "delete" && polygonDeleteActiveRef.current) {
          handleMapCoordinate(coord, latLng);
          return;
        }
        if (modeRef.current !== "delete") {
          handleMapCoordinate(coord, latLng);
          return;
        }
        onDraftEditRef.current({
          action: "delete_segment",
          edgeId: feature.properties.edgeId,
          segmentType: feature.properties.segmentType,
          reason: "ADMIN_click_delete",
        } as EditAction);
      }, { hitArea: useHitArea });
      if (segmentOverlays) {
        overlaysRef.current.push(...segmentOverlays);
        segmentOverlayByEdgeRef.current.set(String(feature.properties.edgeId), segmentOverlays);
      }
    });

    const bridgeFeatures = bridgePayload?.bridges.features ?? [];
    bridgeFeatures.forEach((feature) => {
      const bridge = createBridgeOverlay(feature, mapRef.current!);
      if (bridge) overlaysRef.current.push(...bridge);
    });

    centerMapForPayloadOnce(allSegmentFeatures, bridgeFeatures);
    if (canRenderDetails) {
      renderPendingEditOverlays();
    } else {
      clearPendingEditOverlays();
    }
    renderReferenceOverlays();
    renderSegmentFeatureOverlays();
    syncDeletedSegmentOverlays();
  }, [payload, bridgePayload, detailedSegmentsVisible, mapReady, mode, roadSegmentLayers, toolbarMode]);

  useEffect(() => {
    if (detailedSegmentsVisible) {
      renderPendingEditOverlays();
    } else {
      clearPendingEditOverlays();
    }
    syncDeletedSegmentOverlays();
  }, [draftEdits, detailedSegmentsVisible]);

  useEffect(() => {
    renderReferenceOverlays();
  }, [detailedSegmentsVisible, mode, referenceLayers, roadAttributePayload, stairPayload, audioSignalPayload, brailleBlockPayload]);

  useEffect(() => {
    renderRouteOverlays();
  }, [routeLines, mapReady]);

  useEffect(() => {
    renderRoutePointOverlays();
  }, [routePoints, mapReady]);

  useEffect(() => {
    renderSegmentFeatureOverlays();
  }, [detailedSegmentsVisible, draftEdits, mapReady, payload, segmentFeatureLayers, toolbarMode]);

  useEffect(() => {
    if (!selectedSegment || !detailedSegmentsVisible) {
      selectedSegmentOverlayRef.current?.setMap(null);
      selectedSegmentOverlayRef.current = null;
      return;
    }
    drawSelectedSegment(selectedSegment);
  }, [detailedSegmentsVisible, selectedSegment]);

  function setMode(nextMode: EditorMode) {
    if (!editable && (nextMode === "add" || nextMode === "delete")) {
      return;
    }
    modeRef.current = nextMode;
    setModeState(nextMode);
    if (nextMode !== "add") {
      addPointsRef.current = [];
      clearTempOverlays();
      setPendingAddCount(0);
    }
    if (nextMode !== "delete") setPolygonDeleteActiveState(false);
    if (nextMode === "roadview") {
      showRoadviewPanel(ROADVIEW_DEFAULT_MESSAGE);
    }
  }

  function setAddType(nextAddType: AddType) {
    addTypeRef.current = nextAddType;
    setAddTypeState(nextAddType);
  }

  function shouldRenderDetailedSegments() {
    return detailedSegmentsVisible;
  }

  function handleMapCoordinate(coord: Coord, latLng: unknown) {
    if (routePointPickModeRef.current) {
      onRoutePointPickRef.current?.({ lat: coord[1], lng: coord[0] });
      return;
    }

    if (toolbarMode !== "editor") {
      selectNearestSegment(coord, 35, false);
      return;
    }

    if (!editable && (modeRef.current === "add" || modeRef.current === "delete")) {
      return;
    }

    if (shouldOpenRoadviewForMode(modeRef.current)) {
      showRoadviewAt(latLng);
      return;
    }

    if (modeRef.current === "add") {
      const nextCoord = addTypeRef.current === "CROSS_WALK" ? snapCrossWalkEndpoint(coord) : coord;
      addPointsRef.current = [...addPointsRef.current, nextCoord];
      redrawAddPreview();
      setPendingAddCount(addPointsRef.current.length);
      const result = twoPointAddDraft(addTypeRef.current, addPointsRef.current);
      if (!result.edit) return;
      onDraftEditRef.current(result.edit);
      addPointsRef.current = result.remainingPoints;
      clearTempOverlays();
      setPendingAddCount(result.remainingPoints.length);
      return;
    }

    if (modeRef.current === "delete" && polygonDeleteActiveRef.current) {
      if (polygonPointsRef.current.length >= 5) return;
      polygonPointsRef.current = [...polygonPointsRef.current, coord];
      redrawPolygon();
    }
  }

  function clearTempOverlays() {
    tempOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    tempOverlaysRef.current = [];
    polygonShapeRef.current?.setMap(null);
    polygonShapeRef.current = null;
  }

  function drawPoint(coord: Coord, color: string, radius: number): KakaoOverlay | null {
    if (!window.kakao?.maps || !mapRef.current) return null;
    const circle = new window.kakao.maps.Circle({
      map: mapRef.current,
      center: new window.kakao.maps.LatLng(coord[1], coord[0]),
      radius,
      strokeWeight: 2,
      strokeColor: "#ffffff",
      strokeOpacity: 1,
      fillColor: color,
      fillOpacity: 0.95,
    });
    return circle;
  }

  function renderPendingEditOverlays() {
    if (!window.kakao?.maps || !mapRef.current) return;
    clearPendingEditOverlays();

    draftSegmentFeatures(draftEditsRef.current).forEach((feature) => {
      const segmentOverlays = createSegmentOverlay(feature, mapRef.current!, () => undefined, { draft: true, hitArea: false });
      if (segmentOverlays) pendingEditOverlaysRef.current.push(...segmentOverlays);
      feature.geometry.coordinates.forEach((coord) => {
        const point = drawPoint(coord, "#ef4444", 2);
        if (point) pendingEditOverlaysRef.current.push(point);
      });
    });
  }

  function renderReferenceOverlays() {
    if (!window.kakao?.maps || !mapRef.current) return;
    referenceOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    referenceOverlaysRef.current = [];
    hideRoadAttributeTooltip();
    if (!shouldRenderDetailedSegments()) return;

    if (referenceLayers?.roadAttributes) {
      (roadAttributePayload?.roadAttributes.features ?? []).filter(shouldShowRoadAttributeReference).forEach((feature) => {
        const overlays = createRoadAttributeReferenceOverlay(feature, mapRef.current!, {
          onClick: (coord) => selectNearestSegment(coord, 50),
          onMouseOver: (coord) => showRoadAttributeTooltip(feature, coord),
          onMouseOut: hideRoadAttributeTooltip,
        }, toolbarMode !== "editor");
        if (overlays) referenceOverlaysRef.current.push(...overlays);
      });
    }

    const pointLayers: Array<[boolean | undefined, ReferencePointFeature[], string]> = [
      [referenceLayers?.stairs, stairPayload?.points.features ?? [], "#7c2d12"],
      [referenceLayers?.audioSignals, audioSignalPayload?.points.features ?? [], "#0f766e"],
      [referenceLayers?.brailleBlocks, brailleBlockPayload?.points.features ?? [], "#7c3aed"],
    ];
    pointLayers.forEach(([enabled, features, color]) => {
      if (!enabled) return;
      features.forEach((feature) => {
        const overlay = createReferencePointOverlay(feature, mapRef.current!, color);
        if (overlay) referenceOverlaysRef.current.push(overlay);
      });
    });
  }

  function shouldShowRoadSegmentLayer(feature: SegmentFeature) {
    if (toolbarMode !== "roadSegmentLegend") {
      return true;
    }
    const segmentType = feature.properties.segmentType;
    if (segmentType === "CROSS_WALK" || segmentType === "SIDE_WALK") {
      return roadSegmentLayers.crossWalk;
    }
    if (segmentType === "TRANSITION_CONNECTOR") {
      return roadSegmentLayers.transitionConnector;
    }
    return roadSegmentLayers.sideLine;
  }

  function renderRouteOverlays() {
    if (!window.kakao?.maps || !mapRef.current) return;
    routeOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    routeOverlaysRef.current = [];
    const safeLine = createRoutePolyline(routeLines?.safe ?? [], "#dc2626", 7);
    const fastLine = createRoutePolyline(routeLines?.fast ?? [], "#2563eb", 5);
    if (safeLine) {
      safeLine.setMap(mapRef.current);
      routeOverlaysRef.current.push(safeLine);
    }
    if (fastLine) {
      fastLine.setMap(mapRef.current);
      routeOverlaysRef.current.push(fastLine);
    }
  }

  function renderRoutePointOverlays() {
    if (!window.kakao?.maps || !mapRef.current) return;
    const map = mapRef.current;
    routePointOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    routePointOverlaysRef.current = [];
    const start = routePoints?.start ? createRoutePointOverlay(routePoints.start, "출발", "start", map) : null;
    const end = routePoints?.end ? createRoutePointOverlay(routePoints.end, "도착", "end", map) : null;
    if (start) routePointOverlaysRef.current.push(start);
    if (end) routePointOverlaysRef.current.push(end);
  }

  function renderSegmentFeatureOverlays() {
    if (!window.kakao?.maps || !mapRef.current) return;
    const map = mapRef.current;
    segmentFeatureOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    segmentFeatureOverlaysRef.current = [];
    if (toolbarMode !== "segmentFeatureLegend" || !shouldRenderDetailedSegments()) return;
    const activeTypes = new Set(segmentFeatureTypes.filter((featureType) => segmentFeatureLayers[featureType]));
    if (!activeTypes.size) return;
    const segmentFeatures = visibleSegmentFeatures(payload?.segments.features ?? [], draftEditsRef.current);
    segmentFeatures.forEach((feature) => {
      const overlays = createSegmentFeatureOverlays(feature, activeTypes);
      overlays.forEach((overlay) => {
        overlay.setMap(map);
        segmentFeatureOverlaysRef.current.push(overlay);
      });
    });
  }

  function clearPendingEditOverlays() {
    pendingEditOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    pendingEditOverlaysRef.current = [];
  }

  function centerMapForPayloadOnce(segmentFeatures: SegmentFeature[], bridgeFeatures: BridgeFeature[]) {
    if (centeredPayloadRef.current.payload === payload && centeredPayloadRef.current.bridgePayload === bridgePayload) {
      return;
    }
    centeredPayloadRef.current = { payload, bridgePayload };
    centerMapForPayload(segmentFeatures, bridgeFeatures);
  }

  function centerMapForPayload(segmentFeatures: SegmentFeature[], bridgeFeatures: BridgeFeature[]) {
    if (!window.kakao?.maps || !mapRef.current) return;
    const bbox = payload?.bbox;
    if (bbox && window.kakao.maps.LatLngBounds && mapRef.current.setBounds) {
      const bounds = new window.kakao.maps.LatLngBounds();
      bounds.extend(new window.kakao.maps.LatLng(bbox[1], bbox[0]));
      bounds.extend(new window.kakao.maps.LatLng(bbox[3], bbox[2]));
      mapRef.current.setBounds(bounds);
      return;
    }
    const firstCoord = segmentFeatures[0]?.geometry.coordinates[0] ?? bridgeFeatures[0]?.geometry.coordinates[0];
    if (firstCoord) {
      mapRef.current.setCenter(new window.kakao.maps.LatLng(firstCoord[1], firstCoord[0]));
    }
  }

  function hideRoadAttributeTooltip() {
    roadAttributeTooltipRef.current?.setMap(null);
    roadAttributeTooltipRef.current = null;
  }

  function showRoadAttributeTooltip(feature: RoadAttributeFeature, position: unknown) {
    if (!window.kakao?.maps || !mapRef.current) return;
    hideRoadAttributeTooltip();
    const content = document.createElement("div");
    content.className = "attribute-tooltip";
    content.innerHTML = `
      <strong>${escapeHtml(feature.properties.sourceId || feature.properties.handoffEdgeId)}</strong>
      <span>${escapeHtml(feature.properties.slopeLevelLabel || feature.properties.slopeLevel || "-")} · ${escapeHtml(feature.properties.widthLevelLabel || "-")} · ${escapeHtml(feature.properties.pavementQualityLabel || feature.properties.surfaceType || "-")}</span>
    `;
    roadAttributeTooltipRef.current = new window.kakao.maps.CustomOverlay({
      map: mapRef.current,
      position,
      content,
      xAnchor: 0.5,
      yAnchor: 1.2,
      zIndex: 40,
    });
    window.setTimeout(() => {
      content.parentElement?.style.setProperty("pointer-events", "none");
    }, 0);
  }

  function selectNearestSegment(coord: Coord, maxDistanceM: number, openRoadview = true) {
    if (!shouldRenderDetailedSegments()) return;
    const candidates = visibleSegmentFeatures(payload?.segments.features ?? [], draftEditsRef.current);
    const nearest = candidates
      .map((feature) => ({ feature, distanceM: pointLineDistanceM(coord, feature.geometry.coordinates) }))
      .filter((item) => item.distanceM <= maxDistanceM)
      .sort((left, right) => left.distanceM - right.distanceM)[0];
    if (!nearest) return;
    onSelectSegmentRef.current(nearest.feature);
    drawSelectedSegment(nearest.feature);
    if (openRoadview) showRoadviewAt(kakaoLatLngAtMidpoint(nearest.feature.geometry.coordinates));
  }

  function drawSelectedSegment(feature: SegmentFeature) {
    if (!window.kakao?.maps || !mapRef.current) return;
    selectedSegmentOverlayRef.current?.setMap(null);
    selectedSegmentOverlayRef.current = new window.kakao.maps.Polyline({
      map: mapRef.current,
      path: feature.geometry.coordinates.map(([lng, lat]) => new window.kakao!.maps.LatLng(lat, lng)),
      strokeColor: "#111827",
      strokeWeight: 8,
      strokeOpacity: 0.95,
      strokeStyle: "solid",
      zIndex: 22,
    });
  }

  function syncDeletedSegmentOverlays() {
    const map = mapRef.current;
    if (!map) return;
    const deleted = deletedEdgeIds(draftEditsRef.current);
    segmentOverlayByEdgeRef.current.forEach((overlays, edgeId) => {
      overlays.forEach((overlay) => overlay.setMap(deleted.has(edgeId) ? null : map));
    });
  }

  function redrawAddPreview() {
    clearTempOverlays();
    addPointsRef.current.forEach((coord) => {
      const point = drawPoint(coord, "#ef4444", 2);
      if (point) tempOverlaysRef.current.push(point);
    });
    if (addPointsRef.current.length >= 2) {
      const line = createPolyline(addPointsRef.current, addTypeRef.current, { opacity: 1 });
      if (line) tempOverlaysRef.current.push(line);
    }
  }

  function setPolygonDeleteActiveState(active: boolean) {
    polygonDeleteActiveRef.current = active;
    setPolygonDeleteActive(active);
    polygonPointsRef.current = [];
    setPolygonPointCount(0);
    clearTempOverlays();
  }

  function resetPolygonDeletePoints() {
    const reset = resetPolygonDeleteSelection(polygonDeleteActiveRef.current);
    polygonDeleteActiveRef.current = reset.active;
    setPolygonDeleteActive(reset.active);
    polygonPointsRef.current = reset.points;
    setPolygonPointCount(reset.pointCount);
    clearTempOverlays();
  }

  function redrawPolygon() {
    if (!window.kakao?.maps || !mapRef.current) return;
    clearTempOverlays();
    polygonPointsRef.current.forEach((coord) => {
      const point = drawPoint(coord, "#ef4444", 4);
      if (point) tempOverlaysRef.current.push(point);
    });
    if (polygonPointsRef.current.length >= 2) {
      polygonShapeRef.current = new window.kakao.maps.Polygon({
        map: mapRef.current,
        path: polygonPointsRef.current.map(([lng, lat]) => new window.kakao!.maps.LatLng(lat, lng)),
        strokeColor: "#ef4444",
        strokeWeight: 2,
        strokeOpacity: 0.9,
        fillColor: "#fecaca",
        fillOpacity: 0.2,
      });
    }
    setPolygonPointCount(polygonPointsRef.current.length);
  }

  function deletePolygon() {
    if (polygonPointsRef.current.length < 3) return;
    const candidates = visibleSegmentFeatures(payload?.segments.features ?? [], draftEditsRef.current);
    const matches = segmentsTouchingPolygon(candidates, polygonPointsRef.current);
    matches.forEach((feature) => {
      onDraftEditRef.current({
        action: "delete_segment",
        edgeId: feature.properties.edgeId,
        reason: "manual_polygon_delete",
      });
    });
    resetPolygonDeletePoints();
  }

  function closeRoadviewPanel() {
    onRoadviewChange({ open: false, message: ROADVIEW_DEFAULT_MESSAGE });
    roadviewMarkerRef.current?.setMap(null);
    roadviewMarkerRef.current = null;
    roadviewArrowElRef.current = null;
    if (modeRef.current === "roadview") setMode("idle");
  }

  function showRoadviewPanel(message: string) {
    onRoadviewChange({ open: true, message, onClose: closeRoadviewPanel });
  }

  function setRoadviewPanelMessage(message: string) {
    onRoadviewChange({ open: true, message, onClose: closeRoadviewPanel });
  }

  function updateRoadviewMarkerDirection() {
    if (!roadviewRef.current?.getViewpoint || !roadviewArrowElRef.current) return;
    const viewpoint = roadviewRef.current.getViewpoint();
    const pan = Number(viewpoint?.pan || 0);
    roadviewArrowElRef.current.style.transform = `translate(-13px, -13px) rotate(${pan}deg)`;
  }

  function createRoadviewMarker(latLng: unknown) {
    if (!window.kakao?.maps || !mapRef.current) return;
    roadviewMarkerRef.current?.setMap(null);
    const marker = document.createElement("div");
    marker.className = "roadview-arrow-marker";
    const inner = document.createElement("div");
    inner.className = "roadview-arrow-inner";
    marker.appendChild(inner);
    roadviewArrowElRef.current = marker;
    roadviewMarkerRef.current = new window.kakao.maps.CustomOverlay({
      map: mapRef.current,
      position: latLng,
      content: marker,
      xAnchor: 0.5,
      yAnchor: 0.5,
      zIndex: 8,
    });
    window.setTimeout(() => {
      marker.parentElement?.style.setProperty("pointer-events", "none");
    }, 0);
  }

  function showRoadviewAt(latLng: unknown) {
    if (!window.kakao?.maps) return;
    showRoadviewPanel(ROADVIEW_DEFAULT_MESSAGE);
    const unavailable = roadviewUnavailableMessage(Boolean(roadviewClientRef.current), Boolean(window.kakao.maps.Roadview));
    if (unavailable) {
      setRoadviewPanelMessage(unavailable);
      return;
    }
    if (!roadviewRef.current && roadviewContainerRef.current && window.kakao.maps.Roadview) {
      roadviewRef.current = new window.kakao.maps.Roadview(roadviewContainerRef.current);
      window.kakao.maps.event.addListener(roadviewRef.current, "viewpoint_changed", updateRoadviewMarkerDirection);
      window.kakao.maps.event.addListener(roadviewRef.current, "pano_changed", updateRoadviewMarkerDirection);
    }
    if (!roadviewRef.current || !roadviewClientRef.current) {
      setRoadviewPanelMessage("Kakao Roadview is unavailable.");
      return;
    }
    setRoadviewPanelMessage("Searching nearby Roadview...");
    createRoadviewMarker(latLng);
    roadviewClientRef.current.getNearestPanoId(latLng, 80, (panoId) => {
      if (!panoId) {
        setRoadviewPanelMessage("No Kakao Roadview was found near this point.");
        return;
      }
      setRoadviewPanelMessage("");
      roadviewRef.current?.setPanoId(panoId, latLng);
      window.setTimeout(() => {
        roadviewRef.current?.relayout?.();
        updateRoadviewMarkerDirection();
      }, 0);
    });
  }

  function kakaoLatLngAtMidpoint(coords: Coord[]): unknown {
    const coord = coords[Math.floor(coords.length / 2)] ?? coords[0];
    return new window.kakao!.maps.LatLng(coord[1], coord[0]);
  }

  function snapCrossWalkEndpoint(coord: Coord): Coord {
    const candidates = visibleSegmentFeatures(payload?.segments.features ?? [], draftEditsRef.current)
      .filter((feature) => {
        const segmentType = feature.properties.segmentType;
        return segmentType === "SIDE_LINE" || segmentType === "SIDE_WALK";
      });
    const nearest = nearestPointOnSegments(coord, candidates);
    if (!nearest || nearest.distanceM > 1.5) {
      setSnapMessage(null);
      return coord;
    }
    setSnapMessage(`CROSS_WALK 끝점을 기존 선 위로 ${nearest.distanceM.toFixed(1)}m 보정했습니다.`);
    return nearest.coord;
  }

  const segmentFeatureCounts = countSegmentFeatureTypes(visibleSegmentFeatures(payload?.segments.features ?? [], draftEdits));

  return (
    <section className="map-shell">
      <div ref={containerRef} className="map-canvas" />
      {toolbarMode === "editor" ? (
        <div className="map-toolbar">
          <button className={mode === "delete" ? "selected-tool" : ""} onClick={() => setMode("delete")} disabled={!editable}>Delete</button>
          <button className={mode === "add" ? "selected-tool" : ""} onClick={() => setMode("add")} disabled={!editable}>Add</button>
          <select value={addType} onChange={(event) => setAddType(event.target.value as AddType)} disabled={mode !== "add" || !editable}>
            <option value="SIDE_LINE">SIDE_LINE</option>
            <option value="CROSS_WALK">CROSS_WALK</option>
          </select>
          {mode === "delete" && (
            <>
              <button className={`danger-outline ${polygonDeleteActive ? "selected-tool" : ""}`} onClick={() => setPolygonDeleteActiveState(!polygonDeleteActive)} disabled={!editable}>영역 선택</button>
              <button className="danger-soft" onClick={deletePolygon} disabled={polygonPointCount < 3 || !editable}>영역 삭제</button>
              {polygonDeleteActive && <span className="toolbar-hint">지도에서 3~5개 점을 찍어 삭제 영역을 만듭니다.</span>}
            </>
          )}
          <button className={mode === "roadview" ? "selected-tool" : ""} onClick={() => setMode("roadview")}>Roadview</button>
          <span className="draft-count-badge">변경 {draftEditCount}건</span>
          <button onClick={onUndoDraftEdit} disabled={!draftEditCount || !onUndoDraftEdit}>Undo</button>
          <button onClick={onClearDraftEdits} disabled={!draftEditCount || !onClearDraftEdits}>Clear</button>
        </div>
      ) : toolbarMode === "roadSegmentLegend" ? (
        <div className="map-toolbar attribute-legend">
          <LegendItem
            color="#c9342f"
            label="SIDE_LINE"
            active={roadSegmentLayers.sideLine}
            onClick={() => setRoadSegmentLayers((layers) => ({ ...layers, sideLine: !layers.sideLine }))}
          />
          <LegendItem
            color="#2563eb"
            label="CROSS_WALK / SIDE_WALK"
            active={roadSegmentLayers.crossWalk}
            onClick={() => setRoadSegmentLayers((layers) => ({ ...layers, crossWalk: !layers.crossWalk }))}
          />
          <LegendItem
            color="#64748b"
            label="TRANSITION_CONNECTOR"
            active={roadSegmentLayers.transitionConnector}
            onClick={() => setRoadSegmentLayers((layers) => ({ ...layers, transitionConnector: !layers.transitionConnector }))}
          />
        </div>
      ) : (
        <div className="map-toolbar attribute-legend">
          {segmentFeatureTypes.map((featureType) => (
            <LegendItem
              key={featureType}
              color={segmentFeatureColors[featureType]}
              label={`${segmentFeatureLabels[featureType]} ${segmentFeatureCounts.get(featureType) ?? 0}`}
              active={segmentFeatureLayers[featureType]}
              onClick={() => setSegmentFeatureLayers((layers) => ({ ...layers, [featureType]: !layers[featureType] }))}
            />
          ))}
        </div>
      )}
      <div className="map-status">
        {loading
          ? "loading..."
          : error
            ? `payload 오류: ${error.message}`
            : mapError
              ? `지도 오류: ${mapError}`
              : !detailedSegmentsVisible
                ? `확대하면 보행 네트워크 segment가 표시됩니다. 현재 level ${mapLevel}, 표시 기준 ${DETAIL_SEGMENT_MAX_LEVEL} 이하`
                : `${payload?.summary?.visibleSegmentCount ?? payload?.segments.features.length ?? 0} segments · ${bridgePayload?.summary?.visibleBridgeCandidateCount ?? bridgePayload?.bridges.features.length ?? 0} bridges · ${mode}${pendingAddCount ? ` · add ${pendingAddCount}` : ""}${polygonDeleteActive ? ` · 영역 ${polygonPointCount}/5점` : ""}${snapMessage ? ` · ${snapMessage}` : ""}`}
      </div>
    </section>
  );
}

function LegendItem({
  color,
  label,
  active,
  onClick,
}: {
  color: string;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button type="button" className={`legend-item legend-toggle ${active ? "active" : ""}`} onClick={onClick}>
      <span style={{ backgroundColor: color }} />
      {label}
    </button>
  );
}

function createRoutePolyline(points: GeoPoint[], color: string, strokeWeight: number): KakaoOverlay | null {
  if (!window.kakao?.maps || points.length < 2) return null;
  return new window.kakao.maps.Polyline({
    path: points.map((point) => new window.kakao!.maps.LatLng(point.lat, point.lng)),
    strokeWeight,
    strokeColor: color,
    strokeOpacity: 0.92,
    strokeStyle: "solid",
    zIndex: 30,
  });
}

function createRoutePointOverlay(point: GeoPoint, label: string, type: "start" | "end", map: KakaoMap): KakaoOverlay | null {
  if (!window.kakao?.maps) return null;
  const marker = document.createElement("div");
  marker.className = `route-point-marker ${type}`;
  marker.textContent = label;
  return new window.kakao.maps.CustomOverlay({
    map,
    position: new window.kakao.maps.LatLng(point.lat, point.lng),
    content: marker,
    xAnchor: 0.5,
    yAnchor: 1,
    zIndex: 36,
  });
}

function createSegmentFeatureOverlays(feature: SegmentFeature, activeTypes: Set<SegmentFeatureType>): KakaoOverlay[] {
  if (!window.kakao?.maps) return [];
  const matchedTypes = segmentFeatureTypes.filter((featureType) =>
    activeTypes.has(featureType) && feature.properties.featureTypes?.includes(featureType));
  if (!matchedTypes.length) return [];

  const primaryType = primarySegmentFeatureType(matchedTypes);
  const badge = document.createElement("div");
  badge.className = "segment-feature-badge";
  badge.style.backgroundColor = segmentFeatureColors[primaryType];
  badge.title = matchedTypes.map((featureType) => segmentFeatureLabels[featureType]).join(" / ");
  badge.textContent = matchedTypes.length > 1 ? String(matchedTypes.length) : "";
  const badgeCoord = midpointCoord(feature.geometry.coordinates);

  return [new window.kakao.maps.CustomOverlay({
    position: new window.kakao.maps.LatLng(badgeCoord[1], badgeCoord[0]),
    content: badge,
    xAnchor: 0.5,
    yAnchor: 0.5,
    zIndex: 27,
  })];
}

function primarySegmentFeatureType(featureTypes: SegmentFeatureType[]) {
  const priority: SegmentFeatureType[] = ["STAIRS", "BRAILLE_BLOCK", "AUDIO_SIGNAL", "CROSSWALK"];
  return priority.find((featureType) => featureTypes.includes(featureType)) ?? featureTypes[0];
}

function midpointCoord(coords: Coord[]): Coord {
  return coords[Math.floor(coords.length / 2)] ?? coords[0] ?? [0, 0];
}

function countSegmentFeatureTypes(features: SegmentFeature[]) {
  const counts = new Map<SegmentFeatureType, number>();
  features.forEach((feature) => {
    feature.properties.featureTypes?.forEach((featureType) => {
      counts.set(featureType, (counts.get(featureType) ?? 0) + 1);
    });
  });
  return counts;
}

function createBridgeOverlay(feature: BridgeFeature, map: KakaoMap): KakaoOverlay[] | null {
  if (!window.kakao?.maps) return null;

  const path = feature.geometry.coordinates.map(([lng, lat]) => new window.kakao!.maps.LatLng(lat, lng));
  const line = new window.kakao.maps.Polyline({
    path,
    strokeWeight: feature.properties.priority === "AUTO" ? 4 : 3,
    strokeColor: "#60a5fa",
    strokeOpacity: 0.92,
    strokeStyle: "shortdash",
  });
  line.setMap(map);

  const markerPoint = feature.properties.markerPoint ?? feature.geometry.coordinates[0];
  const marker = new window.kakao.maps.Circle({
    map,
    center: new window.kakao.maps.LatLng(markerPoint[1], markerPoint[0]),
    radius: 4,
    strokeWeight: 2,
    strokeColor: "#ffffff",
    strokeOpacity: 1,
    fillColor: "#2563eb",
    fillOpacity: 0.95,
  });

  return [line, marker];
}

function createRoadAttributeReferenceOverlay(
  feature: RoadAttributeFeature,
  map: KakaoMap,
  handlers: {
    onClick: (coord: Coord) => void;
    onMouseOver: (position: unknown) => void;
    onMouseOut: () => void;
  },
  interactive: boolean,
): KakaoOverlay[] | null {
  if (!window.kakao?.maps) return null;
  const path = feature.geometry.coordinates.map(([lng, lat]) => new window.kakao!.maps.LatLng(lat, lng));
  const visibleLine = new window.kakao.maps.Polyline({
    map,
    path,
    strokeWeight: Math.max(3, roadAttributeStrokeWeight(feature.properties.widthLevel) + 2),
    strokeColor: roadAttributeStrokeColor(feature.properties.slopeLevel),
    strokeOpacity: 0.22,
    strokeStyle: roadAttributeStrokeStyle(feature.properties.surfaceType),
    clickable: false,
    zIndex: 1,
  });

  if (!interactive) return [visibleLine];

  const hitLine = new window.kakao.maps.Polyline({
    map,
    path,
    strokeWeight: Math.max(14, roadAttributeStrokeWeight(feature.properties.widthLevel) + 10),
    strokeColor: roadAttributeStrokeColor(feature.properties.slopeLevel),
    strokeOpacity: 0.01,
    strokeStyle: "solid",
    clickable: true,
    zIndex: 19,
  });

  window.kakao.maps.event.addListener(hitLine, "click", (event: unknown) => {
    const latLng = (event as { latLng?: { getLng: () => number; getLat: () => number } }).latLng;
    if (!latLng) return;
    handlers.onClick([latLng.getLng(), latLng.getLat()]);
  });
  window.kakao.maps.event.addListener(hitLine, "mouseover", (event: unknown) => {
    const position = (event as { latLng?: unknown }).latLng ?? path[Math.floor(path.length / 2)];
    handlers.onMouseOver(position);
  });
  window.kakao.maps.event.addListener(hitLine, "mouseout", handlers.onMouseOut);
  return [visibleLine, hitLine];
}

function createReferencePointOverlay(feature: ReferencePointFeature, map: KakaoMap, color: string): KakaoOverlay | null {
  if (!window.kakao?.maps) return null;
  const marker = document.createElement("span");
  marker.className = "reference-point-marker";
  marker.style.backgroundColor = color;
  marker.title = `${feature.properties.label} ${feature.properties.sourceId}`;
  return new window.kakao.maps.CustomOverlay({
    map,
    position: new window.kakao.maps.LatLng(feature.geometry.coordinates[1], feature.geometry.coordinates[0]),
    content: marker,
    xAnchor: 0.5,
    yAnchor: 0.5,
    zIndex: 12,
  });
}

function createSegmentOverlay(
  feature: SegmentFeature,
  map: KakaoMap,
  onClick: (coord: Coord, latLng: unknown) => void,
  options: { draft?: boolean; hitArea?: boolean } = {},
): KakaoOverlay[] | null {
  const line = createPolyline(feature.geometry.coordinates, feature.properties.segmentType ?? "SIDE_LINE", {
    draft: options.draft,
    opacity: options.draft ? 0.98 : undefined,
  });
  if (!line || !window.kakao?.maps) return null;
  const overlays = [line];
  const handleClick = (event: unknown) => {
    const latLng = (event as { latLng?: { getLng: () => number; getLat: () => number } }).latLng ?? kakaoLatLngAtCoord(feature.geometry.coordinates[Math.floor(feature.geometry.coordinates.length / 2)] ?? feature.geometry.coordinates[0]);
    onClick([latLng.getLng(), latLng.getLat()], latLng);
  };
  window.kakao.maps.event.addListener(line, "click", handleClick);

  if (options.hitArea !== false) {
    const path = feature.geometry.coordinates.map(([lng, lat]) => new window.kakao!.maps.LatLng(lat, lng));
    const hitLine = new window.kakao.maps.Polyline({
      path,
      strokeWeight: feature.properties.segmentType === "CROSS_WALK" ? 18 : 20,
      strokeColor: "#111827",
      strokeOpacity: 0.01,
      strokeStyle: "solid",
      clickable: true,
      zIndex: 21,
    });
    window.kakao.maps.event.addListener(hitLine, "click", handleClick);
    hitLine.setMap(map);
    overlays.push(hitLine);
  }

  line.setMap(map);
  return overlays;
}

function kakaoLatLngAtCoord(coord: Coord): { getLng: () => number; getLat: () => number } {
  const latLng = new window.kakao!.maps.LatLng(coord[1], coord[0]);
  return latLng as { getLng: () => number; getLat: () => number };
}

function createPolyline(
  coordinates: Coord[],
  segmentType: string,
  options: { draft?: boolean; opacity?: number } = {},
): KakaoOverlay | null {
  if (!window.kakao?.maps) return null;

  const path = coordinates.map(([lng, lat]) => new window.kakao!.maps.LatLng(lat, lng));
  const isCrossWalk = segmentType === "CROSS_WALK" || segmentType === "SIDE_WALK";
  const strokeColor = isCrossWalk ? "#2563eb" : segmentType === "TRANSITION_CONNECTOR" ? "#64748b" : "#c9342f";
  const strokeWeight = isCrossWalk ? 5 : 4;
  const opacity = options.opacity ?? (segmentType === "TRANSITION_CONNECTOR" ? 0.45 : 0.88);
  const zIndex = options.draft ? 18 : segmentType === "TRANSITION_CONNECTOR" ? 8 : isCrossWalk ? 16 : 14;

  return new window.kakao.maps.Polyline({
    path,
    strokeWeight,
    strokeColor,
    strokeOpacity: opacity,
    strokeStyle: options.draft ? "shortdash" : "solid",
    clickable: true,
    zIndex,
  });
}

function nearestPointOnSegments(point: Coord, segments: SegmentFeature[]): { coord: Coord; distanceM: number } | null {
  let nearest: { coord: Coord; distanceM: number } | null = null;
  segments.forEach((feature) => {
    const coordinates = feature.geometry.coordinates;
    coordinates.slice(1).forEach((coord, index) => {
      const projected = nearestPointOnLineSegment(point, coordinates[index], coord);
      if (!nearest || projected.distanceM < nearest.distanceM) {
        nearest = projected;
      }
    });
  });
  return nearest;
}

function nearestPointOnLineSegment(point: Coord, start: Coord, end: Coord): { coord: Coord; distanceM: number } {
  const originLat = point[1];
  const pointMeters = lngLatToLocalMeters(point, originLat);
  const startMeters = lngLatToLocalMeters(start, originLat);
  const endMeters = lngLatToLocalMeters(end, originLat);
  const dx = endMeters.x - startMeters.x;
  const dy = endMeters.y - startMeters.y;
  const lengthSquared = dx * dx + dy * dy;
  const t = lengthSquared === 0
    ? 0
    : Math.max(0, Math.min(1, ((pointMeters.x - startMeters.x) * dx + (pointMeters.y - startMeters.y) * dy) / lengthSquared));
  const projectedMeters = {
    x: startMeters.x + dx * t,
    y: startMeters.y + dy * t,
  };
  return {
    coord: localMetersToLngLat(projectedMeters, originLat),
    distanceM: Math.hypot(pointMeters.x - projectedMeters.x, pointMeters.y - projectedMeters.y),
  };
}

function lngLatToLocalMeters([lng, lat]: Coord, originLat: number) {
  const metersPerDegreeLat = 111_320;
  const metersPerDegreeLng = 111_320 * Math.cos((originLat * Math.PI) / 180);
  return {
    x: lng * metersPerDegreeLng,
    y: lat * metersPerDegreeLat,
  };
}

function localMetersToLngLat(point: { x: number; y: number }, originLat: number): Coord {
  const metersPerDegreeLat = 111_320;
  const metersPerDegreeLng = 111_320 * Math.cos((originLat * Math.PI) / 180);
  return [point.x / metersPerDegreeLng, point.y / metersPerDegreeLat];
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
