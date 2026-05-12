import { useEffect, useRef, useState } from "react";
import { previewAdminRoute } from "../api/adminApi";
import type {
  AdminRoutePreviewResponse,
  AdminRouteTuningRequest,
  GeoPoint,
  WalkRouteProfile,
} from "../types";
import { loadKakaoMap, type KakaoMap, type KakaoOverlay } from "../map/kakaoLoader";

type PointMode = "start" | "end";

const profileOptions: Array<{ value: WalkRouteProfile; label: string }> = [
  { value: "PEDESTRIAN_SAFE", label: "일반 보행 안전" },
  { value: "PEDESTRIAN_FAST", label: "일반 보행 최단" },
  { value: "VISUAL_SAFE", label: "저시력 안전" },
  { value: "VISUAL_FAST", label: "저시력 최단" },
  { value: "WHEELCHAIR_MANUAL_SAFE", label: "수동 휠체어 안전" },
  { value: "WHEELCHAIR_MANUAL_FAST", label: "수동 휠체어 최단" },
  { value: "WHEELCHAIR_AUTO_SAFE", label: "전동 휠체어 안전" },
  { value: "WHEELCHAIR_AUTO_FAST", label: "전동 휠체어 최단" },
];

const manualWheelchairSafeTuning: AdminRouteTuningRequest = {
  slopeLowPercent: 3,
  slopeMiddlePercent: 5.56,
  slopeHighPercent: 8.33,
  slopeLowPenalty: 0.45,
  slopeMiddlePenalty: 0.1,
  slopeHighPenalty: 0.05,
  narrowWidthPenalty: 0.12,
  unpavedSurfacePenalty: 0.25,
  stairsPenalty: 0,
  signalCrosswalkBonus: 1.05,
  distanceInfluence: 50,
};

const profileDefaultTunings: Record<WalkRouteProfile, AdminRouteTuningRequest> = {
  PEDESTRIAN_SAFE: {
    ...manualWheelchairSafeTuning,
    slopeLowPenalty: 1,
    slopeMiddlePenalty: 0.85,
    slopeHighPenalty: 0.6,
    narrowWidthPenalty: 0.9,
    unpavedSurfacePenalty: 0.75,
    stairsPenalty: 0.55,
    signalCrosswalkBonus: 1.1,
    distanceInfluence: 40,
  },
  PEDESTRIAN_FAST: {
    ...manualWheelchairSafeTuning,
    slopeLowPenalty: 1,
    slopeMiddlePenalty: 0.95,
    slopeHighPenalty: 0.85,
    narrowWidthPenalty: 0.95,
    unpavedSurfacePenalty: 0.85,
    stairsPenalty: 0.75,
    signalCrosswalkBonus: 1,
    distanceInfluence: 90,
  },
  VISUAL_SAFE: {
    ...manualWheelchairSafeTuning,
    slopeLowPenalty: 0.75,
    slopeMiddlePenalty: 0.5,
    slopeHighPenalty: 0.2,
    narrowWidthPenalty: 0.75,
    unpavedSurfacePenalty: 0.7,
    stairsPenalty: 0.45,
    signalCrosswalkBonus: 1.1,
    distanceInfluence: 35,
  },
  VISUAL_FAST: {
    ...manualWheelchairSafeTuning,
    slopeLowPenalty: 0.9,
    slopeMiddlePenalty: 0.75,
    slopeHighPenalty: 0.6,
    narrowWidthPenalty: 0.9,
    unpavedSurfacePenalty: 0.85,
    stairsPenalty: 0.75,
    signalCrosswalkBonus: 1,
    distanceInfluence: 90,
  },
  WHEELCHAIR_MANUAL_SAFE: manualWheelchairSafeTuning,
  WHEELCHAIR_MANUAL_FAST: {
    ...manualWheelchairSafeTuning,
    slopeLowPenalty: 0.65,
    slopeMiddlePenalty: 0.25,
    slopeHighPenalty: 0.15,
    narrowWidthPenalty: 0.25,
    unpavedSurfacePenalty: 0.45,
    stairsPenalty: 0,
    signalCrosswalkBonus: 1,
    distanceInfluence: 110,
  },
  WHEELCHAIR_AUTO_SAFE: {
    ...manualWheelchairSafeTuning,
    slopeLowPercent: 5.56,
    slopeMiddlePercent: 8.33,
    slopeHighPercent: 10,
    slopeLowPenalty: 0.65,
    slopeMiddlePenalty: 0.25,
    slopeHighPenalty: 0.1,
    narrowWidthPenalty: 0.2,
    unpavedSurfacePenalty: 0.35,
    stairsPenalty: 0,
    signalCrosswalkBonus: 1.05,
    distanceInfluence: 45,
  },
  WHEELCHAIR_AUTO_FAST: {
    ...manualWheelchairSafeTuning,
    slopeLowPercent: 5.56,
    slopeMiddlePercent: 8.33,
    slopeHighPercent: 10,
    slopeLowPenalty: 0.8,
    slopeMiddlePenalty: 0.45,
    slopeHighPenalty: 0.3,
    narrowWidthPenalty: 0.35,
    unpavedSurfacePenalty: 0.55,
    stairsPenalty: 0,
    signalCrosswalkBonus: 1,
    distanceInfluence: 95,
  },
};

const tuningFields: Array<{
  key: keyof AdminRouteTuningRequest;
  label: string;
  min: number;
  max: number;
  step: number;
}> = [
  { key: "slopeLowPercent", label: "완만 경사 시작 (%)", min: 0, max: 15, step: 0.1 },
  { key: "slopeMiddlePercent", label: "중간 경사 시작 (%)", min: 0, max: 15, step: 0.1 },
  { key: "slopeHighPercent", label: "급경사 시작 (%)", min: 0, max: 15, step: 0.1 },
  { key: "slopeLowPenalty", label: "완만 경사 배율", min: 0, max: 2, step: 0.01 },
  { key: "slopeMiddlePenalty", label: "중간 경사 배율", min: 0, max: 2, step: 0.01 },
  { key: "slopeHighPenalty", label: "급경사 배율", min: 0, max: 2, step: 0.01 },
  { key: "narrowWidthPenalty", label: "좁은 보도 배율", min: 0, max: 2, step: 0.01 },
  { key: "unpavedSurfacePenalty", label: "비포장 배율", min: 0, max: 2, step: 0.01 },
  { key: "stairsPenalty", label: "계단 배율", min: 0, max: 2, step: 0.01 },
  { key: "signalCrosswalkBonus", label: "신호 횡단보도 배율", min: 0, max: 2, step: 0.01 },
  { key: "distanceInfluence", label: "거리 영향도", min: 0, max: 200, step: 1 },
];

export function RouteTuningPage({ accessToken }: { accessToken: string }) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const overlaysRef = useRef<KakaoOverlay[]>([]);
  const modeRef = useRef<PointMode>("start");
  const [mode, setMode] = useState<PointMode>("start");
  const [startPoint, setStartPoint] = useState<GeoPoint | null>(null);
  const [endPoint, setEndPoint] = useState<GeoPoint | null>(null);
  const [profile, setProfile] = useState<WalkRouteProfile>("WHEELCHAIR_MANUAL_SAFE");
  const [tuning, setTuning] = useState<AdminRouteTuningRequest>(profileDefaultTunings.WHEELCHAIR_MANUAL_SAFE);
  const [result, setResult] = useState<AdminRoutePreviewResponse | null>(null);
  const [status, setStatus] = useState("시작점과 도착점을 지도에서 선택하세요.");
  const [mapError, setMapError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    modeRef.current = mode;
  }, []);

  useEffect(() => {
    let disposed = false;
    loadKakaoMap()
      .then(() => {
        if (disposed || !containerRef.current || mapRef.current || !window.kakao?.maps) return;
        mapRef.current = new window.kakao.maps.Map(containerRef.current, {
          center: new window.kakao.maps.LatLng(35.1796, 129.0756),
          level: 6,
        });
        window.kakao.maps.event.addListener(mapRef.current, "click", (event: unknown) => {
          const latLng = (event as { latLng?: { getLng: () => number; getLat: () => number } }).latLng;
          if (!latLng) return;
          const point = { lat: latLng.getLat(), lng: latLng.getLng() };
          if (modeRef.current === "start") {
            setStartPoint(point);
            setMode("end");
            setStatus("도착점을 선택하세요.");
            return;
          }
          setEndPoint(point);
          setStatus("경로 미리보기를 실행할 수 있습니다.");
        });
      })
      .catch((reason: Error) => setMapError(reason.message));
    return () => {
      disposed = true;
    };
  }, [mode]);

  useEffect(() => {
    drawPreview();
  }, [startPoint, endPoint, result]);

  function updateTuning(key: keyof AdminRouteTuningRequest, value: number) {
    setTuning((current) => ({ ...current, [key]: value }));
    setResult(null);
    setStatus("수치가 변경되었습니다. 경로 비교를 다시 실행하세요.");
  }

  async function previewRoute() {
    if (!startPoint || !endPoint) {
      setStatus("시작점과 도착점을 먼저 선택하세요.");
      return;
    }
    if (tuning.slopeLowPercent >= tuning.slopeMiddlePercent || tuning.slopeMiddlePercent >= tuning.slopeHighPercent) {
      setStatus("경사도 임계값은 낮음 < 중간 < 높음 순서여야 합니다.");
      return;
    }
    setLoading(true);
    setStatus("GraphHopper 경로를 계산하는 중입니다.");
    try {
      const response = await previewAdminRoute({ startPoint, endPoint, profile, tuning }, accessToken);
      setResult(response);
      setStatus("기본 경로와 조정 경로를 표시했습니다.");
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "경로 미리보기 실패");
    } finally {
      setLoading(false);
    }
  }

  function resetPoints() {
    setStartPoint(null);
    setEndPoint(null);
    setResult(null);
    setMode("start");
    setStatus("시작점과 도착점을 지도에서 선택하세요.");
  }

  function drawPreview() {
    if (!window.kakao?.maps || !mapRef.current) return;
    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = [];
    if (startPoint) {
      const marker = drawPoint(startPoint, "#16a34a");
      if (marker) overlaysRef.current.push(marker);
    }
    if (endPoint) {
      const marker = drawPoint(endPoint, "#dc2626");
      if (marker) overlaysRef.current.push(marker);
    }
    const baseLine = drawLine(result?.baseRoute.coordinates ?? [], "#64748b", 5, 0.7);
    const tunedLine = drawLine(result?.tunedRoute.coordinates ?? [], "#2563eb", 7, 0.92);
    if (baseLine) overlaysRef.current.push(baseLine);
    if (tunedLine) overlaysRef.current.push(tunedLine);
  }

  function drawPoint(point: GeoPoint, color: string) {
    if (!window.kakao?.maps || !mapRef.current) return null;
    return new window.kakao.maps.Circle({
      map: mapRef.current,
      center: new window.kakao.maps.LatLng(point.lat, point.lng),
      radius: 7,
      strokeWeight: 2,
      strokeColor: "#ffffff",
      strokeOpacity: 1,
      fillColor: color,
      fillOpacity: 0.95,
    });
  }

  function drawLine(points: GeoPoint[], color: string, weight: number, opacity: number) {
    if (!window.kakao?.maps || !mapRef.current || points.length < 2) return null;
    return new window.kakao.maps.Polyline({
      map: mapRef.current,
      path: points.map((point) => new window.kakao!.maps.LatLng(point.lat, point.lng)),
      strokeColor: color,
      strokeWeight: weight,
      strokeOpacity: opacity,
      strokeStyle: "solid",
      zIndex: weight,
    });
  }

  return (
    <div className="route-tuning-layout">
      <section className="route-tuning-map-panel">
        <div ref={containerRef} className="route-tuning-map" />
        <div className="map-toolbar">
          <button className={mode === "start" ? "selected-tool" : ""} type="button" onClick={() => setMode("start")}>
            시작점
          </button>
          <button className={mode === "end" ? "selected-tool" : ""} type="button" onClick={() => setMode("end")}>
            도착점
          </button>
          <button type="button" onClick={resetPoints}>초기화</button>
        </div>
        <div className="map-status">{mapError ? `지도 오류: ${mapError}` : status}</div>
      </section>
      <aside className="route-tuning-panel">
        <section className="panel-section">
          <h3>프로필</h3>
          <label className="route-profile-select">
            GraphHopper profile
            <select
              value={profile}
              onChange={(event) => {
                const nextProfile = event.target.value as WalkRouteProfile;
                setProfile(nextProfile);
                setTuning(profileDefaultTunings[nextProfile]);
                setResult(null);
                setStatus("프로필이 변경되었습니다. 경로 비교를 다시 실행하세요.");
              }}
            >
              {profileOptions.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>
        </section>

        <section className="panel-section">
          <h3>튜닝 수치</h3>
          <div className="tuning-control-list">
            {tuningFields.map((field) => (
              <label key={field.key} className="tuning-control">
                <span>{field.label}</span>
                <input
                  type="range"
                  min={field.min}
                  max={field.max}
                  step={field.step}
                  value={tuning[field.key]}
                  onChange={(event) => updateTuning(field.key, Number(event.target.value))}
                />
                <input
                  type="number"
                  min={field.min}
                  max={field.max}
                  step={field.step}
                  value={tuning[field.key]}
                  onChange={(event) => updateTuning(field.key, Number(event.target.value))}
                />
              </label>
            ))}
          </div>
          <div className="button-row">
            <button className="primary" type="button" onClick={previewRoute} disabled={loading || !startPoint || !endPoint}>
              {loading ? "계산 중" : "경로 비교"}
            </button>
          </div>
        </section>

        <section className="panel-section">
          <h3>비교 결과</h3>
          {result ? (
            <div className="route-result-grid">
              <RouteResultCard label="기본" color="#64748b" route={result.baseRoute} />
              <RouteResultCard label="조정" color="#2563eb" route={result.tunedRoute} />
            </div>
          ) : (
            <p className="muted">시작점/도착점과 수치를 정한 뒤 경로 비교를 실행하세요.</p>
          )}
        </section>
      </aside>
    </div>
  );
}

function RouteResultCard({
  label,
  color,
  route,
}: {
  label: string;
  color: string;
  route: AdminRoutePreviewResponse["baseRoute"];
}) {
  return (
    <div className="route-result-card">
      <span style={{ borderColor: color }}>{label}</span>
      <strong>{formatDistance(route.distanceMeter)}</strong>
      <small>{route.estimatedTimeMinute}분 · {route.durationSecond}초</small>
    </div>
  );
}

function formatDistance(distanceMeter: number) {
  if (distanceMeter >= 1000) {
    return `${(distanceMeter / 1000).toFixed(2)}km`;
  }
  return `${distanceMeter.toFixed(0)}m`;
}
