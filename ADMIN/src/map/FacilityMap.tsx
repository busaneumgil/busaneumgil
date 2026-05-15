import { type RefObject, useEffect, useRef, useState } from "react";
import type { FacilityFeature, FacilityPayload } from "../types";
import { attachKakaoWheelZoom, loadKakaoMap, type KakaoMap, type KakaoOverlay, type KakaoRoadview, type KakaoRoadviewClient } from "./kakaoLoader";
import { facilityCategoryColor, facilityCategoryLabel } from "./facilityStyle";
import { roadviewUnavailableMessage } from "./roadviewMode";
import type { RoadviewDockState } from "./SegmentMap";
import type { PlaceCategory } from "../types";

interface FacilityMapProps {
  payload?: FacilityPayload;
  loading: boolean;
  error?: Error | null;
  selectedFeature: FacilityFeature | null;
  onSelectFeature: (feature: FacilityFeature) => void;
  roadviewContainerRef: RefObject<HTMLDivElement | null>;
  onRoadviewChange: (state: RoadviewDockState) => void;
  locationPickEnabled?: boolean;
  onPickLocation?: (point: { lat: number; lng: number }) => void;
}

const ROADVIEW_DEFAULT_MESSAGE = "편의시설 점을 클릭하면 근처 Roadview를 엽니다.";
const facilityCategories: PlaceCategory[] = ["PUBLIC_OFFICE", "WELFARE", "HEALTHCARE", "TOURIST_SPOT", "FOOD_CAFE", "ACCOMMODATION", "ETC"];

export function FacilityMap({
  payload,
  loading,
  error,
  selectedFeature,
  onSelectFeature,
  roadviewContainerRef,
  onRoadviewChange,
  locationPickEnabled = false,
  onPickLocation,
}: FacilityMapProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const detachWheelZoomRef = useRef<(() => void) | null>(null);
  const roadviewRef = useRef<KakaoRoadview | null>(null);
  const roadviewClientRef = useRef<KakaoRoadviewClient | null>(null);
  const roadviewMarkerRef = useRef<KakaoOverlay | null>(null);
  const roadviewArrowElRef = useRef<HTMLDivElement | null>(null);
  const overlaysRef = useRef<KakaoOverlay[]>([]);
  const selectedOverlayRef = useRef<KakaoOverlay | null>(null);
  const tooltipRef = useRef<KakaoOverlay | null>(null);
  const onSelectFeatureRef = useRef(onSelectFeature);
  const locationPickEnabledRef = useRef(locationPickEnabled);
  const onPickLocationRef = useRef(onPickLocation);
  const centeredPayloadKeyRef = useRef<string | null>(null);
  const [mapError, setMapError] = useState<string | null>(null);
  const [categoryLayers, setCategoryLayers] = useState<Record<PlaceCategory, boolean>>({
    PUBLIC_OFFICE: true,
    WELFARE: true,
    HEALTHCARE: true,
    TOURIST_SPOT: true,
    FOOD_CAFE: true,
    ACCOMMODATION: true,
    ETC: true,
  });

  useEffect(() => {
    onSelectFeatureRef.current = onSelectFeature;
    locationPickEnabledRef.current = locationPickEnabled;
    onPickLocationRef.current = onPickLocation;
  }, [locationPickEnabled, onPickLocation, onSelectFeature]);

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
        detachWheelZoomRef.current?.();
        detachWheelZoomRef.current = attachKakaoWheelZoom(containerRef.current, () => mapRef.current);
        roadviewClientRef.current = window.kakao.maps.RoadviewClient ? new window.kakao.maps.RoadviewClient() : null;
        window.kakao.maps.event.addListener(mapRef.current, "click", (event: unknown) => {
          if (!locationPickEnabledRef.current) return;
          const latLng = (event as { latLng?: { getLng: () => number; getLat: () => number } }).latLng;
          if (!latLng) return;
          onPickLocationRef.current?.({ lat: latLng.getLat(), lng: latLng.getLng() });
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
    if (!mapRef.current || !window.kakao?.maps) return;

    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = [];
    selectedOverlayRef.current?.setMap(null);
    selectedOverlayRef.current = null;
    tooltipRef.current?.setMap(null);
    tooltipRef.current = null;

    const allFeatures = payload?.facilities.features ?? [];
    const features = allFeatures.filter((feature) => categoryLayers[feature.properties.category] ?? true);
    features.forEach((feature) => {
      const overlay = createFacilityOverlay(feature, mapRef.current!, {
        onClick: () => {
          onSelectFeatureRef.current(feature);
          drawSelectedFeature(feature);
          showRoadviewAt(kakaoLatLng(feature));
        },
        onMouseOver: () => showTooltip(feature),
        onMouseOut: hideTooltip,
      });
      if (overlay) overlaysRef.current.push(overlay);
    });
    if (selectedFeature && (categoryLayers[selectedFeature.properties.category] ?? true)) {
      drawSelectedFeature(selectedFeature);
    }

    centerMapForPayloadOnce(features);
  }, [categoryLayers, payload, selectedFeature]);

  useEffect(() => {
    if (!selectedFeature) {
      selectedOverlayRef.current?.setMap(null);
      selectedOverlayRef.current = null;
      return;
    }
    drawSelectedFeature(selectedFeature);
  }, [selectedFeature]);

  function hideTooltip() {
    tooltipRef.current?.setMap(null);
    tooltipRef.current = null;
  }

  function showTooltip(feature: FacilityFeature) {
    if (!window.kakao?.maps || !mapRef.current) return;
    hideTooltip();
    const content = document.createElement("div");
    content.className = "attribute-tooltip";
    content.innerHTML = `
      <strong>${escapeHtml(feature.properties.name || feature.properties.placeId)}</strong>
      <span>${escapeHtml(facilityCategoryLabel(feature.properties.category))} · ${escapeHtml(feature.properties.address || "-")}</span>
    `;
    tooltipRef.current = new window.kakao.maps.CustomOverlay({
      map: mapRef.current,
      position: kakaoLatLng(feature),
      content,
      xAnchor: 0.5,
      yAnchor: 1.35,
      zIndex: 30,
    });
  }

  function drawSelectedFeature(feature: FacilityFeature) {
    if (!window.kakao?.maps || !mapRef.current) return;
    selectedOverlayRef.current?.setMap(null);
    selectedOverlayRef.current = new window.kakao.maps.Circle({
      map: mapRef.current,
      center: kakaoLatLng(feature),
      radius: 18,
      strokeWeight: 3,
      strokeColor: "#111827",
      strokeOpacity: 0.95,
      fillColor: "#ffffff",
      fillOpacity: 0.25,
      zIndex: 20,
    });
  }

  function centerMapForPayloadOnce(features: FacilityFeature[]) {
    if (!window.kakao?.maps || !mapRef.current) return;
    const bbox = payload?.bbox;
    const firstCoord = features[0]?.geometry.coordinates;
    const nextKey = bbox ? bbox.join(",") : firstCoord?.join(",");
    if (!nextKey || centeredPayloadKeyRef.current === nextKey) {
      return;
    }
    centeredPayloadKeyRef.current = nextKey;
    if (bbox && window.kakao.maps.LatLngBounds && mapRef.current.setBounds) {
      const bounds = new window.kakao.maps.LatLngBounds();
      bounds.extend(new window.kakao.maps.LatLng(bbox[1], bbox[0]));
      bounds.extend(new window.kakao.maps.LatLng(bbox[3], bbox[2]));
      mapRef.current.setBounds(bounds);
      return;
    }
    if (firstCoord) {
      mapRef.current.setCenter(new window.kakao.maps.LatLng(firstCoord[1], firstCoord[0]));
    }
  }

  function closeRoadviewPanel() {
    onRoadviewChange({ open: false, message: ROADVIEW_DEFAULT_MESSAGE });
    roadviewMarkerRef.current?.setMap(null);
    roadviewMarkerRef.current = null;
    roadviewArrowElRef.current = null;
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
  }

  function showRoadviewAt(latLng: unknown) {
    if (!window.kakao?.maps) return;
    setRoadviewPanelMessage(ROADVIEW_DEFAULT_MESSAGE);
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
        setRoadviewPanelMessage("No Kakao Roadview was found near this facility.");
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

  function kakaoLatLng(feature: FacilityFeature): unknown {
    const [lng, lat] = feature.geometry.coordinates;
    return new window.kakao!.maps.LatLng(lat, lng);
  }

  const visibleCounts = payload?.summary?.visibleCategoryCounts ?? {};
  const visibleFacilityCount = (payload?.facilities.features ?? []).filter((feature) => categoryLayers[feature.properties.category] ?? true).length;

  return (
    <section className="map-shell">
      <div ref={containerRef} className="map-canvas" />
      <div className="map-toolbar attribute-legend">
        {facilityCategories.map((category) => (
          <LegendItem
            key={category}
            color={facilityCategoryColor(category)}
            label={`${facilityCategoryLabel(category)} ${visibleCounts[category] ?? 0}`}
            active={categoryLayers[category]}
            onClick={() => setCategoryLayers((layers) => ({ ...layers, [category]: !layers[category] }))}
          />
        ))}
      </div>
      <div className="map-status">
        {loading
          ? "loading..."
          : error
            ? `편의시설 오류: ${error.message}`
            : mapError
              ? `지도 오류: ${mapError}`
              : `${visibleFacilityCount} / ${payload?.summary?.visibleFacilityCount ?? payload?.facilities.features.length ?? 0} facilities${selectedFeature ? ` · selected ${selectedFeature.properties.placeId}` : ""}`}
      </div>
    </section>
  );
}

function createFacilityOverlay(
  feature: FacilityFeature,
  map: KakaoMap,
  handlers: {
    onClick: () => void;
    onMouseOver: () => void;
    onMouseOut: () => void;
  },
): KakaoOverlay | null {
  if (!window.kakao?.maps) return null;

  const marker = document.createElement("button");
  marker.type = "button";
  marker.className = "facility-marker";
  marker.style.backgroundColor = facilityCategoryColor(feature.properties.category);
  marker.title = feature.properties.name;
  marker.addEventListener("click", handlers.onClick);
  marker.addEventListener("mouseenter", handlers.onMouseOver);
  marker.addEventListener("mouseleave", handlers.onMouseOut);

  const overlay = new window.kakao.maps.CustomOverlay({
    map,
    position: new window.kakao.maps.LatLng(feature.geometry.coordinates[1], feature.geometry.coordinates[0]),
    content: marker,
    xAnchor: 0.5,
    yAnchor: 0.5,
    zIndex: 10,
  });
  return overlay;
}

function LegendItem({ color, label, active, onClick }: { color: string; label: string; active: boolean; onClick: () => void }) {
  return (
    <button type="button" className={`legend-item legend-toggle ${active ? "active" : ""}`} onClick={onClick}>
      <span style={{ background: color }} />
      {label}
    </button>
  );
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
