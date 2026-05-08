import { useEffect, useMemo, useRef, useState } from "react";
import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import {
  backendApiUrl,
  fetchAdminAreas,
  fetchAdminFacilityPayload,
  fetchAdminRoadNetworkPayload,
  getStoredAdminAccessToken,
  normalizeAdminAccessToken,
  storeAdminAccessToken,
} from "./api/adminApi";
import { adminShellClassName } from "./layout/adminLayout";
import { FacilityMap } from "./map/FacilityMap";
import { facilityCategoryLabel } from "./map/facilityStyle";
import { SegmentMap, type RoadviewDockState } from "./map/SegmentMap";
import { HazardReportsPage } from "./report/HazardReportsPage";
import { useAdminStore } from "./store/adminStore";
import type { AdminPage, FacilityFeature, SegmentFeature } from "./types";

const pageMeta: Record<AdminPage, { label: string; description: string }> = {
  network: {
    label: "보행 네트워크",
    description: "SIDE_LINE/CROSS_WALK를 구·동 단위로 편집하고 CSV 반영 전 draft를 검수합니다.",
  },
  facilities: {
    label: "편의시설",
    description: "보행약자 편의시설 위치와 접근성 속성을 검수합니다.",
  },
  hazards: {
    label: "제보 관리",
    description: "사용자가 등록한 도로 상태 제보를 확인하고 승인 또는 반려합니다.",
  },
};

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function AdminApp() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const roadviewContainerRef = useRef<HTMLDivElement | null>(null);
  const [roadviewDock, setRoadviewDock] = useState<RoadviewDockState>({
    open: false,
    message: "Roadview 도구를 누른 뒤 지도를 클릭하면 Kakao Roadview를 엽니다.",
  });
  const [selectedFacility, setSelectedFacility] = useState<FacilityFeature | null>(null);
  const [selectedSegment, setSelectedSegment] = useState<SegmentFeature | null>(null);
  const [accessToken, setAccessToken] = useState(getStoredAdminAccessToken);
  const [tokenInput, setTokenInput] = useState(accessToken);
  const {
    page,
    selectedGu,
    selectedDong,
    draftEdits,
    setPage,
    setSelectedArea,
    undoDraftEdit,
    clearDraft,
    requestReview,
    addDraftEdit,
  } = useAdminStore();

  const hasToken = Boolean(accessToken);

  useEffect(() => {
    setRoadviewDock({
      open: false,
      message:
        page === "facilities"
            ? "편의시설 점을 클릭하면 근처 Roadview를 엽니다."
            : "Roadview 도구를 누른 뒤 지도를 클릭하면 Kakao Roadview를 엽니다.",
    });
  }, [page, selectedGu, selectedDong]);

  const areasQuery = useQuery({
    queryKey: ["admin-areas", accessToken],
    queryFn: () => fetchAdminAreas(accessToken),
    enabled: page !== "hazards" && hasToken,
    retry: false,
  });

  const payloadQuery = useQuery({
    queryKey: ["admin-road-network", selectedGu, selectedDong, accessToken],
    queryFn: () => fetchAdminRoadNetworkPayload({ gu: selectedGu, dong: selectedDong, accessToken }),
    enabled: page === "network" && hasToken,
    retry: false,
  });

  const facilityQuery = useQuery({
    queryKey: ["admin-facilities", accessToken],
    queryFn: () => fetchAdminFacilityPayload({ accessToken }),
    enabled: page === "facilities" && hasToken,
    retry: false,
  });

  function saveToken() {
    const nextToken = normalizeAdminAccessToken(tokenInput);
    storeAdminAccessToken(nextToken);
    setAccessToken(nextToken);
    setTokenInput(nextToken);
  }

  function clearToken() {
    storeAdminAccessToken("");
    setAccessToken("");
    setTokenInput("");
  }

  const filteredDongs = useMemo(() => {
    const areas = areasQuery.data ?? [];
    return areas.filter((area) => area.gu === selectedGu);
  }, [areasQuery.data, selectedGu]);

  return (
    <div className={adminShellClassName(sidebarCollapsed)}>
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">B</span>
          <div className="brand-copy">
            <strong>부산이음길</strong>
            <small>Admin</small>
          </div>
          <button
            className="sidebar-toggle"
            type="button"
            aria-label={sidebarCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
            onClick={() => setSidebarCollapsed((value) => !value)}
          >
            <span aria-hidden="true" />
          </button>
        </div>
        <nav>
          {(Object.keys(pageMeta) as AdminPage[]).map((item) => (
            <button key={item} className={page === item ? "active" : ""} onClick={() => setPage(item)} title={pageMeta[item].label}>
              <span className="nav-dot" aria-hidden="true" />
              <span className="nav-label">{pageMeta[item].label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <h1>{pageMeta[page].label}</h1>
            <p>{pageMeta[page].description}</p>
          </div>
          {page !== "hazards" && <div className="topbar-actions">
            <label className="backend-field">
              Backend
              <span>{backendApiUrl}</span>
            </label>
            <label className="token-field">
              Access Token
              <input
                type="password"
                value={tokenInput}
                placeholder="ADMIN accessToken"
                onChange={(event) => setTokenInput(event.target.value)}
              />
            </label>
            <button className="primary" type="button" onClick={saveToken}>적용</button>
            <button type="button" onClick={clearToken}>제거</button>
            <label>
              구
              <select
                value={selectedGu}
                onChange={(event) => {
                  const nextGu = event.target.value;
                  const nextDong = (areasQuery.data ?? []).find((area) => area.gu === nextGu)?.dong ?? "";
                  setSelectedArea(nextGu, nextDong);
                  setSelectedFacility(null);
                  setSelectedSegment(null);
                }}
              >
                {[...new Set((areasQuery.data ?? []).map((area) => area.gu))]
                  .filter(Boolean)
                  .map((gu) => (
                    <option key={gu} value={gu}>
                      {gu}
                    </option>
                  ))}
                {!areasQuery.data?.length && <option value={selectedGu}>{selectedGu}</option>}
              </select>
            </label>
            <label>
              동
              <select
                value={selectedDong}
                onChange={(event) => {
                  setSelectedArea(selectedGu, event.target.value);
                  setSelectedFacility(null);
                  setSelectedSegment(null);
                }}
              >
                {filteredDongs.map((area) => (
                  <option key={`${area.gu}-${area.dong}`} value={area.dong}>
                    {area.dong}
                  </option>
                ))}
                {!filteredDongs.length && <option value={selectedDong}>{selectedDong}</option>}
              </select>
            </label>
          </div>}
        </header>

        {page === "hazards" && <HazardReportsPage />}

        {page === "network" && (
          <div className="editor-layout">
            <SegmentMap
              payload={payloadQuery.data}
              loading={payloadQuery.isLoading}
              error={payloadQuery.error}
              draftEdits={draftEdits}
              onDraftEdit={addDraftEdit}
              selectedSegment={selectedSegment}
              onSelectSegment={setSelectedSegment}
              roadviewContainerRef={roadviewContainerRef}
              onRoadviewChange={setRoadviewDock}
            />
            <aside className="detail-panel">
              <section className="panel-section roadview-dock-section">
                <div className="roadview-panel docked">
                  <div className="roadview-header">
                    <span>Roadview</span>
                    <button
                      className="roadview-close"
                      type="button"
                      aria-label="Close roadview"
                      onClick={() => roadviewDock.onClose?.()}
                      disabled={!roadviewDock.open}
                    >
                      x
                    </button>
                  </div>
                  <div className="roadview-body">
                    <div ref={roadviewContainerRef} className="roadview-container" />
                    {roadviewDock.message && <div className="roadview-empty">{roadviewDock.message}</div>}
                  </div>
                </div>
              </section>
              <section className="panel-section">
                <h3>선택 segment</h3>
                {selectedSegment ? (
                  <SegmentReferenceDetails segment={selectedSegment} />
                ) : (
                  <p className="muted">Select 모드에서 segment를 클릭하면 DB에 저장된 기본 정보를 표시합니다.</p>
                )}
              </section>
              <section className="panel-section">
                <h3>변경 draft</h3>
                <div className="metric-grid">
                  <Metric label="현재 edits" value={draftEdits.length} />
                  <Metric label="visible" value={payloadQuery.data?.summary?.visibleSegmentCount ?? "-"} />
                  <Metric label="전체" value={payloadQuery.data?.summary?.segmentCount ?? "-"} />
                </div>
                <div className="button-row">
                  <button onClick={undoDraftEdit} disabled={!draftEdits.length}>Undo</button>
                  <button onClick={clearDraft} disabled={!draftEdits.length}>Clear</button>
                </div>
                <ol className="draft-list">
                  {draftEdits.slice(-8).map((edit, index) => (
                    <li key={`${edit.action}-${index}`}>
                      {edit.action} {"edgeId" in edit ? edit.edgeId : "segmentType" in edit ? edit.segmentType : ""}
                    </li>
                  ))}
                </ol>
              </section>
              <section className="panel-section">
                <h3>검수 흐름</h3>
                <button className="primary" onClick={requestReview}>
                  Request Review
                </button>
                <p className="muted">DB 수정 반영은 후속 API에서 처리합니다. 현재 화면은 DB 조회와 로컬 draft 확인만 지원합니다.</p>
              </section>
            </aside>
          </div>
        )}

        {page === "facilities" && (
          <div className="editor-layout">
            <FacilityMap
              payload={facilityQuery.data}
              loading={facilityQuery.isLoading}
              error={facilityQuery.error}
              selectedFeature={selectedFacility}
              onSelectFeature={setSelectedFacility}
              roadviewContainerRef={roadviewContainerRef}
              onRoadviewChange={setRoadviewDock}
            />
            <aside className="detail-panel">
              <section className="panel-section roadview-dock-section">
                <div className="roadview-panel docked">
                  <div className="roadview-header">
                    <span>Roadview</span>
                    <button
                      className="roadview-close"
                      type="button"
                      aria-label="Close roadview"
                      onClick={() => roadviewDock.onClose?.()}
                      disabled={!roadviewDock.open}
                    >
                      x
                    </button>
                  </div>
                  <div className="roadview-body">
                    <div ref={roadviewContainerRef} className="roadview-container" />
                    {roadviewDock.message && <div className="roadview-empty">{roadviewDock.message}</div>}
                  </div>
                </div>
              </section>
              <section className="panel-section">
                <h3>편의시설</h3>
                <div className="metric-grid">
                  <Metric label="visible" value={facilityQuery.data?.summary?.visibleFacilityCount ?? "-"} />
                  <Metric label="전체" value={facilityQuery.data?.summary?.facilityCount ?? "-"} />
                  <Metric label="provider" value={facilityQuery.data?.summary?.providerPlaceIdCount ?? "-"} />
                </div>
                {selectedFacility ? (
                  <FacilityDetails feature={selectedFacility} />
                ) : (
                  <p className="muted">지도에서 편의시설 점을 hover하면 요약을 보고, 클릭하면 상세와 Roadview를 고정합니다.</p>
                )}
              </section>
              <section className="panel-section">
                <h3>분류</h3>
                <div className="filter-chip-list">
                  {Object.entries(facilityQuery.data?.summary?.visibleCategoryCounts ?? {}).map(([category, count]) => (
                    <span key={category}>{facilityCategoryLabel(category)} {count}</span>
                  ))}
                  {!Object.keys(facilityQuery.data?.summary?.visibleCategoryCounts ?? {}).length && <span>로딩 전</span>}
                </div>
              </section>
            </aside>
          </div>
        )}

      </main>
    </div>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AdminApp />
    </QueryClientProvider>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function SegmentReferenceDetails({ segment }: { segment: SegmentFeature }) {
  return (
    <>
      <dl className="attribute-detail-list">
        <AttributeRow label="edge" value={String(segment.properties.edgeId)} />
        <AttributeRow label="from" value={String(segment.properties.fromNodeId ?? "-")} />
        <AttributeRow label="to" value={String(segment.properties.toNodeId ?? "-")} />
        <AttributeRow label="type" value={String(segment.properties.segmentType ?? "-")} />
        <AttributeRow label="length" value={`${formatNumber(Number(segment.properties.lengthMeter))}m`} />
      </dl>
      <p className="muted">CSV 서버를 거치지 않고 DB road_segments 기준으로 조회한 값입니다.</p>
    </>
  );
}

function FacilityDetails({ feature }: { feature: FacilityFeature }) {
  const properties = feature.properties;
  return (
    <dl className="attribute-detail-list">
      <AttributeRow label="place" value={properties.placeId} />
      <AttributeRow label="provider" value={properties.providerPlaceId || "-"} />
      <AttributeRow label="이름" value={properties.name || "-"} />
      <AttributeRow label="분류" value={facilityCategoryLabel(properties.category)} />
      <AttributeRow label="주소" value={properties.address || "-"} />
    </dl>
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

function formatNumber(value?: number | null) {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  return value.toFixed(value % 1 === 0 ? 0 : 1);
}

export default App;
