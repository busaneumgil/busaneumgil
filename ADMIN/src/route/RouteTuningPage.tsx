import { type RefObject, useEffect, useState } from "react";
import { previewAdminRoute, updateAdminRoadSegmentAttributes } from "../api/adminApi";
import { SegmentMap, type RoadviewDockState } from "../map/SegmentMap";
import type {
  AccessibilityState,
  AdminRoutePreviewResponse,
  AdminRouteProfileGroup,
  AdminRoadSegmentAttributesUpdateRequest,
  GeoPoint,
  SegmentFeature,
  SegmentPayload,
  SurfaceState,
  WidthState,
  SegmentFeatureType,
} from "../types";

type PointMode = "start" | "end";

const profileGroups: Array<{ value: AdminRouteProfileGroup; label: string }> = [
  { value: "PEDESTRIAN", label: "일반 보행" },
  { value: "VISUAL", label: "저시력" },
  { value: "WHEELCHAIR_MANUAL", label: "수동 휠체어" },
  { value: "WHEELCHAIR_AUTO", label: "전동 휠체어" },
];

const accessibilityOptions: AccessibilityState[] = ["YES", "NO", "UNKNOWN"];
const widthOptions: WidthState[] = ["ADEQUATE_150", "ADEQUATE_120", "NARROW", "UNKNOWN"];
const surfaceOptions: SurfaceState[] = ["PAVED", "UNPAVED", "UNKNOWN"];
const segmentFeatureLabels: Record<SegmentFeatureType, string> = {
  CROSSWALK: "횡단보도",
  AUDIO_SIGNAL: "음향신호기",
  BRAILLE_BLOCK: "점자블록",
  STAIRS: "계단",
};

export function RouteTuningPage({
  accessToken,
  gu,
  dong,
  payload,
  loading,
  error,
  selectedSegment,
  onSelectSegment,
  canEdit,
  assignmentMessage,
  roadviewContainerRef,
  onRoadviewChange,
  onSegmentUpdated,
}: {
  accessToken: string;
  gu: string;
  dong: string;
  payload?: SegmentPayload;
  loading: boolean;
  error?: Error | null;
  selectedSegment: SegmentFeature | null;
  onSelectSegment: (segment: SegmentFeature | null) => void;
  canEdit: boolean;
  assignmentMessage: string;
  roadviewContainerRef: RefObject<HTMLDivElement | null>;
  onRoadviewChange: (state: RoadviewDockState) => void;
  onSegmentUpdated: () => void;
}) {
  const [pointMode, setPointMode] = useState<PointMode>("start");
  const [routePickEnabled, setRoutePickEnabled] = useState(false);
  const [startPoint, setStartPoint] = useState<GeoPoint | null>(null);
  const [endPoint, setEndPoint] = useState<GeoPoint | null>(null);
  const [profileGroup, setProfileGroup] = useState<AdminRouteProfileGroup>("WHEELCHAIR_MANUAL");
  const [preview, setPreview] = useState<AdminRoutePreviewResponse | null>(null);
  const [message, setMessage] = useState("시작점과 도착점을 지도에서 선택하세요.");
  const [previewLoading, setPreviewLoading] = useState(false);
  const [savingAttributes, setSavingAttributes] = useState(false);
  const [attributeDraft, setAttributeDraft] = useState<AdminRoadSegmentAttributesUpdateRequest>({});

  useEffect(() => {
    if (!selectedSegment) {
      setAttributeDraft({});
      return;
    }
    setAttributeDraft({
      walkAccess: normalizeAccessibility(selectedSegment.properties.walkAccess),
      brailleBlockState: normalizeAccessibility(selectedSegment.properties.brailleBlockState),
      audioSignalState: normalizeAccessibility(selectedSegment.properties.audioSignalState),
      widthState: normalizeWidth(selectedSegment.properties.widthState),
      surfaceState: normalizeSurface(selectedSegment.properties.surfaceState),
      stairsState: normalizeAccessibility(selectedSegment.properties.stairsState),
      signalState: normalizeAccessibility(selectedSegment.properties.signalState),
    });
  }, [selectedSegment]);

  function handleRoutePointPick(point: GeoPoint) {
    if (pointMode === "start") {
      setStartPoint(point);
      setPointMode("end");
      setMessage("도착점을 선택하세요.");
      return;
    }
    setEndPoint(point);
    setRoutePickEnabled(false);
    setMessage("경로 미리보기를 실행할 수 있습니다.");
  }

  async function previewRoute() {
    if (!startPoint || !endPoint) {
      setMessage("시작점과 도착점을 먼저 선택하세요.");
      return;
    }
    setPreviewLoading(true);
    setMessage("DB 보행 네트워크 기준 안전/빠른 경로를 계산하는 중입니다.");
    try {
      const result = await previewAdminRoute({ gu, dong, startPoint, endPoint, profileGroup }, accessToken);
      setPreview(result);
      setMessage("빨간선은 안전 경로, 파란선은 빠른 경로입니다.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "경로 미리보기 실패");
    } finally {
      setPreviewLoading(false);
    }
  }

  async function saveSegmentAttributes() {
    if (!selectedSegment) return;
    setSavingAttributes(true);
    try {
      await updateAdminRoadSegmentAttributes(selectedSegment.properties.edgeId, gu, dong, attributeDraft, accessToken);
      setMessage("segment 속성을 저장했습니다. DB 기준 경로 미리보기에는 바로 반영됩니다.");
      onSegmentUpdated();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "segment 속성 저장 실패");
    } finally {
      setSavingAttributes(false);
    }
  }

  function resetRoutePoints() {
    setStartPoint(null);
    setEndPoint(null);
    setPreview(null);
    setPointMode("start");
    setRoutePickEnabled(false);
    setMessage("시작점과 도착점을 지도에서 선택하세요.");
  }

  return (
    <div className="editor-layout">
      <SegmentMap
        payload={payload}
        loading={loading}
        error={error}
        draftEdits={[]}
        onDraftEdit={() => undefined}
        selectedSegment={selectedSegment}
        onSelectSegment={onSelectSegment}
        roadviewContainerRef={roadviewContainerRef}
        onRoadviewChange={onRoadviewChange}
        editable={false}
        toolbarMode="roadSegmentLegend"
        routePointPickMode={routePickEnabled ? pointMode : null}
        onRoutePointPick={handleRoutePointPick}
        routeLines={{
          safe: preview?.safeRoute.coordinates,
          fast: preview?.fastRoute.coordinates,
        }}
      />
      <aside className="detail-panel">
        <SegmentFeatureLegend payload={payload} selectedSegment={selectedSegment} />
        <section className="panel-section">
          <h3>경로 확인</h3>
          <p className="muted">{message}</p>
          <label className="route-profile-select">
            프로필
            <select value={profileGroup} onChange={(event) => setProfileGroup(event.target.value as AdminRouteProfileGroup)}>
              {profileGroups.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
          </label>
          <div className="button-row">
            <button
              type="button"
              className={routePickEnabled && pointMode === "start" ? "selected-tool" : ""}
              onClick={() => {
                setRoutePickEnabled(true);
                setPointMode("start");
              }}
            >
              시작점 선택
            </button>
            <button
              type="button"
              className={routePickEnabled && pointMode === "end" ? "selected-tool" : ""}
              onClick={() => {
                setRoutePickEnabled(true);
                setPointMode("end");
              }}
            >
              도착점 선택
            </button>
            <button type="button" onClick={resetRoutePoints}>초기화</button>
          </div>
          <div className="button-row">
            <button className="primary" type="button" onClick={previewRoute} disabled={previewLoading || !startPoint || !endPoint}>
              {previewLoading ? "계산 중" : "안전/빠른 경로 비교"}
            </button>
          </div>
          {preview && (
            <div className="route-result-grid">
              <RouteResultCard label="빠른" color="#2563eb" route={preview.fastRoute} />
              <RouteResultCard label="안전" color="#dc2626" route={preview.safeRoute} />
            </div>
          )}
        </section>

        <section className="panel-section">
          <h3>segment 속성</h3>
          <p className={canEdit ? "muted" : "error-box"}>{assignmentMessage}</p>
          {selectedSegment ? (
            <>
              <dl className="attribute-detail-list">
                <AttributeRow label="edge" value={String(selectedSegment.properties.edgeId)} />
                <AttributeRow label="type" value={String(selectedSegment.properties.segmentType ?? "-")} />
                <AttributeRow label="length" value={`${formatNumber(Number(selectedSegment.properties.lengthMeter))}m`} />
              </dl>
              <div className="admin-form-grid">
                <StateSelect label="통행 가능" value={attributeDraft.walkAccess ?? "UNKNOWN"} options={accessibilityOptions} disabled={!canEdit} onChange={(value) => setAttributeDraft((draft) => ({ ...draft, walkAccess: value as AccessibilityState }))} />
                <StateSelect label="계단" value={attributeDraft.stairsState ?? "UNKNOWN"} options={accessibilityOptions} disabled={!canEdit} onChange={(value) => setAttributeDraft((draft) => ({ ...draft, stairsState: value as AccessibilityState }))} />
                <StateSelect label="신호등" value={attributeDraft.signalState ?? "UNKNOWN"} options={accessibilityOptions} disabled={!canEdit} onChange={(value) => setAttributeDraft((draft) => ({ ...draft, signalState: value as AccessibilityState }))} />
                <StateSelect label="음향신호기" value={attributeDraft.audioSignalState ?? "UNKNOWN"} options={accessibilityOptions} disabled={!canEdit} onChange={(value) => setAttributeDraft((draft) => ({ ...draft, audioSignalState: value as AccessibilityState }))} />
                <StateSelect label="점자블록" value={attributeDraft.brailleBlockState ?? "UNKNOWN"} options={accessibilityOptions} disabled={!canEdit} onChange={(value) => setAttributeDraft((draft) => ({ ...draft, brailleBlockState: value as AccessibilityState }))} />
                <StateSelect label="보도 폭" value={attributeDraft.widthState ?? "UNKNOWN"} options={widthOptions} disabled={!canEdit} onChange={(value) => setAttributeDraft((draft) => ({ ...draft, widthState: value as WidthState }))} />
                <StateSelect label="노면" value={attributeDraft.surfaceState ?? "UNKNOWN"} options={surfaceOptions} disabled={!canEdit} onChange={(value) => setAttributeDraft((draft) => ({ ...draft, surfaceState: value as SurfaceState }))} />
              </div>
              <p className="muted">저장 값은 DB road_segments에 반영됩니다. 이 화면의 경로 미리보기는 DB 기준으로 계산되며, 운영 앱 경로는 graph-cache 재생성 전까지 기존 캐시 기준일 수 있습니다.</p>
              <div className="button-row">
                <button className="primary" type="button" onClick={saveSegmentAttributes} disabled={!canEdit || savingAttributes}>
                  {savingAttributes ? "저장 중" : "segment 속성 저장"}
                </button>
              </div>
            </>
          ) : (
            <p className="muted">지도에서 segment를 클릭하면 속성을 수정할 수 있습니다.</p>
          )}
        </section>
      </aside>
    </div>
  );
}

function SegmentFeatureLegend({
  payload,
  selectedSegment,
}: {
  payload?: SegmentPayload;
  selectedSegment: SegmentFeature | null;
}) {
  const counts = new Map<SegmentFeatureType, number>();
  (payload?.segments.features ?? []).forEach((segment) => {
    (segment.properties.featureTypes ?? []).forEach((featureType) => {
      counts.set(featureType, (counts.get(featureType) ?? 0) + 1);
    });
  });
  const selectedTypes = new Set(selectedSegment?.properties.featureTypes ?? []);

  return (
    <section className="panel-section">
      <h3>segment_features</h3>
      <div className="legend-chip-row">
        {(Object.keys(segmentFeatureLabels) as SegmentFeatureType[]).map((featureType) => (
          <span key={featureType} className={selectedTypes.has(featureType) ? "legend-chip active" : "legend-chip"}>
            {segmentFeatureLabels[featureType]} {counts.get(featureType) ?? 0}
          </span>
        ))}
      </div>
      <p className="muted">선택한 segment에 포함된 feature는 강조 표시됩니다.</p>
    </section>
  );
}

function StateSelect({
  label,
  value,
  options,
  disabled,
  onChange,
}: {
  label: string;
  value: string;
  options: string[];
  disabled: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <label>
      {label}
      <select value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option} value={option}>{option}</option>
        ))}
      </select>
    </label>
  );
}

function RouteResultCard({
  label,
  color,
  route,
}: {
  label: string;
  color: string;
  route: AdminRoutePreviewResponse["safeRoute"];
}) {
  return (
    <div className="route-result-card">
      <span style={{ borderColor: color }}>{label}</span>
      <strong>{formatDistance(Number(route.distanceMeter))}</strong>
      <small>{route.profile} · {route.estimatedTimeMinute}분</small>
    </div>
  );
}

function AttributeRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function normalizeAccessibility(value: unknown): AccessibilityState {
  return value === "YES" || value === "NO" ? value : "UNKNOWN";
}

function normalizeWidth(value: unknown): WidthState {
  return value === "ADEQUATE_150" || value === "ADEQUATE_120" || value === "NARROW" ? value : "UNKNOWN";
}

function normalizeSurface(value: unknown): SurfaceState {
  return value === "PAVED" || value === "UNPAVED" ? value : "UNKNOWN";
}

function formatDistance(distanceMeter: number) {
  if (distanceMeter >= 1000) {
    return `${(distanceMeter / 1000).toFixed(2)}km`;
  }
  return `${distanceMeter.toFixed(0)}m`;
}

function formatNumber(value?: number | null) {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  return value.toFixed(value % 1 === 0 ? 0 : 1);
}
