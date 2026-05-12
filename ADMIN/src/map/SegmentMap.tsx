import { type RefObject, useEffect, useRef, useState } from "react";
import type { BridgeFeature, BridgePayload, EditableSegmentType, EditAction, GeoPoint, ReferenceLayerKey, ReferencePointFeature, ReferencePointPayload, RoadAttributeFeature, RoadAttributePayload, SegmentFeature, SegmentPayload } from "../types";
import { loadKakaoMap, type KakaoMap, type KakaoOverlay, type KakaoRoadview, type KakaoRoadviewClient } from "./kakaoLoader";
import { deletedEdgeIds, draftSegmentFeatures, resetPolygonDeleteSelection, segmentsTouchingPolygon, twoPointAddDraft, visibleSegmentFeatures } from "./draftSegments";
import { shouldShowRoadAttributeReference } from "./networkReferenceLayer";
import { roadAttributeStrokeColor, roadAttributeStrokeStyle, roadAttributeStrokeWeight } from "./roadAttributeStyle";
import { roadviewUnavailableMessage, shouldOpenRoadviewForMode } from "./roadviewMode";
import { pointLineDistanceM } from "./referenceMatching";

type Coord = [number, number];
type EditorMode = "select" | "delete" | "add" | "roadview";
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
  toolbarMode?: "editor" | "roadSegmentLegend";
}

export interface RoadviewDockState {
  open: boolean;
  message: string;
  onClose?: () => void;
}

const ROADVIEW_DEFAULT_MESSAGE = "Roadview 도구를 누른 뒤 지도를 클릭하면 Kakao Roadview를 엽니다.";

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
  toolbarMode = "editor",
}: SegmentMapProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const roadviewRef = useRef<KakaoRoadview | null>(null);
  const roadviewClientRef = useRef<KakaoRoadviewClient | null>(null);
  const roadviewMarkerRef = useRef<KakaoOverlay | null>(null);
  const roadviewArrowElRef = useRef<HTMLDivElement | null>(null);
  const overlaysRef = useRef<KakaoOverlay[]>([]);
  const tempOverlaysRef = useRef<KakaoOverlay[]>([]);
  const pendingEditOverlaysRef = useRef<KakaoOverlay[]>([]);
  const referenceOverlaysRef = useRef<KakaoOverlay[]>([]);
  const routeOverlaysRef = useRef<KakaoOverlay[]>([]);
  const selectedSegmentOverlayRef = useRef<KakaoOverlay | null>(null);
  const roadAttributeTooltipRef = useRef<KakaoOverlay | null>(null);
  const segmentOverlayByEdgeRef = useRef<Map<string, KakaoOverlay[]>>(new Map());
  const polygonShapeRef = useRef<KakaoOverlay | null>(null);
  const draftEditsRef = useRef<EditAction[]>(draftEdits);
  const modeRef = useRef<EditorMode>("select");
  const addTypeRef = useRef<AddType>("SIDE_LINE");
  const addPointsRef = useRef<Array<[number, number]>>([]);
  const polygonPointsRef = useRef<Coord[]>([]);
  const polygonDeleteActiveRef = useRef(false);
  const onDraftEditRef = useRef(onDraftEdit);
  const onSelectSegmentRef = useRef(onSelectSegment);
  const routePointPickModeRef = useRef(routePointPickMode);
  const onRoutePointPickRef = useRef(onRoutePointPick);
  const [mode, setModeState] = useState<EditorMode>("select");
  const [addType, setAddTypeState] = useState<AddType>("SIDE_LINE");
  const [pendingAddCount, setPendingAddCount] = useState(0);
  const [mapError, setMapError] = useState<string | null>(null);
  const [mapReady, setMapReady] = useState(false);
  const [polygonDeleteActive, setPolygonDeleteActive] = useState(false);
  const [polygonPointCount, setPolygonPointCount] = useState(0);
  const [roadSegmentLayers, setRoadSegmentLayers] = useState({
    sideLine: true,
    crossWalk: true,
    transitionConnector: true,
  });

  useEffect(() => {
    onDraftEditRef.current = onDraftEdit;
    onSelectSegmentRef.current = onSelectSegment;
    draftEditsRef.current = draftEdits;
    routePointPickModeRef.current = routePointPickMode;
    onRoutePointPickRef.current = onRoutePointPick;
  }, [draftEdits, onDraftEdit, onRoutePointPick, onSelectSegment, routePointPickMode]);

  useEffect(() => {
    if (!editable && (modeRef.current === "add" || modeRef.current === "delete")) {
      setMode("select");
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
        roadviewClientRef.current = window.kakao.maps.RoadviewClient ? new window.kakao.maps.RoadviewClient() : null;
        setMapReady(true);
        window.kakao.maps.event.addListener(mapRef.current, "click", (event: unknown) => {
          const latLng = (event as { latLng?: { getLng: () => number; getLat: () => number } }).latLng;
          if (!latLng) return;
          const coord: [number, number] = [latLng.getLng(), latLng.getLat()];
          handleMapCoordinate(coord, latLng);
        });
      })
      .catch((reason: Error) => setMapError(reason.message));

    return () => {
      disposed = true;
    };
  }, []);

  useEffect(() => {
    if (!mapReady || !mapRef.current || !window.kakao?.maps) return;

    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = [];
    segmentOverlayByEdgeRef.current.clear();

    const segmentFeatures = visibleSegmentFeatures(payload?.segments.features ?? [], draftEditsRef.current)
      .filter(shouldShowRoadSegmentLayer);
    segmentFeatures.forEach((feature) => {
      const segmentOverlays = createSegmentOverlay(feature, mapRef.current!, (coord, latLng) => {
        if (routePointPickModeRef.current) {
          handleMapCoordinate(coord, latLng);
          return;
        }
        if (modeRef.current === "select") {
          onSelectSegmentRef.current(feature);
          drawSelectedSegment(feature);
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
      });
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

    centerMapForPayload(segmentFeatures, bridgeFeatures);
    renderPendingEditOverlays();
    renderReferenceOverlays();
    syncDeletedSegmentOverlays();
  }, [payload, bridgePayload, mapReady, roadSegmentLayers]);

  useEffect(() => {
    renderPendingEditOverlays();
    syncDeletedSegmentOverlays();
  }, [draftEdits]);

  useEffect(() => {
    renderReferenceOverlays();
  }, [mode, referenceLayers, roadAttributePayload, stairPayload, audioSignalPayload, brailleBlockPayload]);

  useEffect(() => {
    renderRouteOverlays();
  }, [routeLines, mapReady]);

  useEffect(() => {
    if (!selectedSegment) {
      selectedSegmentOverlayRef.current?.setMap(null);
      selectedSegmentOverlayRef.current = null;
      return;
    }
    drawSelectedSegment(selectedSegment);
  }, [selectedSegment]);

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

  function handleMapCoordinate(coord: Coord, latLng: unknown) {
    if (routePointPickModeRef.current) {
      onRoutePointPickRef.current?.({ lat: coord[1], lng: coord[0] });
      return;
    }

    if (modeRef.current === "select") {
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
      addPointsRef.current = [...addPointsRef.current, coord];
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
    pendingEditOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    pendingEditOverlaysRef.current = [];

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

    if (referenceLayers?.roadAttributes) {
      (roadAttributePayload?.roadAttributes.features ?? []).filter(shouldShowRoadAttributeReference).forEach((feature) => {
        const overlays = createRoadAttributeReferenceOverlay(feature, mapRef.current!, {
          onClick: (coord) => selectNearestSegment(coord, 50),
          onMouseOver: (coord) => showRoadAttributeTooltip(feature, coord),
          onMouseOut: hideRoadAttributeTooltip,
        }, modeRef.current === "select");
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
    if (modeRef.current === "roadview") setMode("select");
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

  return (
    <section className="map-shell">
      <div ref={containerRef} className="map-canvas" />
      {toolbarMode === "editor" ? (
        <div className="map-toolbar">
          <button className={mode === "select" ? "selected-tool" : ""} onClick={() => setMode("select")}>Select</button>
          <button className={mode === "delete" ? "selected-tool" : ""} onClick={() => setMode("delete")} disabled={!editable}>Delete</button>
          <button className={mode === "add" ? "selected-tool" : ""} onClick={() => setMode("add")} disabled={!editable}>Add</button>
          <select value={addType} onChange={(event) => setAddType(event.target.value as AddType)} disabled={mode !== "add" || !editable}>
            <option value="SIDE_LINE">SIDE_LINE</option>
            <option value="CROSS_WALK">CROSS_WALK</option>
          </select>
          {mode === "delete" && (
            <>
              <button className={`danger-outline ${polygonDeleteActive ? "selected-tool" : ""}`} onClick={() => setPolygonDeleteActiveState(!polygonDeleteActive)} disabled={!editable}>Drag</button>
              <button className="danger-soft" onClick={deletePolygon} disabled={polygonPointCount < 3 || !editable}>Delete all</button>
            </>
          )}
          <button className={mode === "roadview" ? "selected-tool" : ""} onClick={() => setMode("roadview")}>Roadview</button>
        </div>
      ) : (
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
      )}
      <div className="map-status">
        {loading
          ? "loading..."
          : error
            ? `payload 오류: ${error.message}`
            : mapError
              ? `지도 오류: ${mapError}`
              : `${payload?.summary?.visibleSegmentCount ?? payload?.segments.features.length ?? 0} segments · ${bridgePayload?.summary?.visibleBridgeCandidateCount ?? bridgePayload?.bridges.features.length ?? 0} bridges · ${mode}${pendingAddCount ? ` · add ${pendingAddCount}` : ""}${polygonDeleteActive ? ` · polygon ${polygonPointCount}/5` : ""}`}
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

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
