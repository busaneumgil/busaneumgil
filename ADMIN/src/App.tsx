import { useEffect, useMemo, useRef, useState } from "react";
import { QueryClient, QueryClientProvider, useMutation, useQuery } from "@tanstack/react-query";
import {
  createAdminRoadNetworkEditJob,
  fetchAdminAreas,
  fetchAdminFacilityPayload,
  fetchAdminPlaceDetail,
  fetchAdminRoadNetworkPayload,
  fetchAdminRoadNetworkEditJob,
  adminAccessTokenRefreshedEvent,
  getStoredAdminAccessToken,
  logoutAdminSession,
  storeAdminAccessToken,
  updateAdminPlace,
  updateAdminPlaceAccessibilityFeatures,
} from "./api/adminApi";
import { AdminAuthPanel } from "./auth/AdminAuthPanel";
import { adminShellClassName } from "./layout/adminLayout";
import { FacilityMap } from "./map/FacilityMap";
import { facilityCategoryLabel } from "./map/facilityStyle";
import { SegmentMap, type RoadviewDockState } from "./map/SegmentMap";
import { HazardReportsPage } from "./report/HazardReportsPage";
import { useAdminStore } from "./store/adminStore";
import type {
  AccessibilityFeatureType,
  AdminMeResponse,
  AdminPage,
  AdminPlaceDetailResponse,
  AdminPlaceUpdateRequest,
  FacilityFeature,
  PlaceAccessibilityFeature,
  PlaceCategory,
  RoadNetworkEditJobResponse,
  SegmentFeature,
} from "./types";

const placeCategories: PlaceCategory[] = [
  "FOOD_CAFE",
  "TOURIST_SPOT",
  "ACCOMMODATION",
  "HEALTHCARE",
  "WELFARE",
  "PUBLIC_OFFICE",
  "ETC",
];

const accessibilityFeatureTypes: AccessibilityFeatureType[] = [
  "accessibleEntrance",
  "elevator",
  "accessibleToilet",
  "accessibleParking",
  "chargingStation",
  "accessibleRoom",
  "guidanceFacility",
];

const pageMeta: Record<AdminPage, { label: string; description: string }> = {
  network: {
    label: "보행 네트워크",
    description: "SIDE_LINE/CROSS_WALK를 구·동 단위로 편집하고 DB 반영 전 draft를 검수합니다.",
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
  const [adminPrincipal, setAdminPrincipal] = useState<AdminMeResponse | null>(null);
  const [activeRoadEditJobId, setActiveRoadEditJobId] = useState<number | null>(null);
  const [lastRoadEditJob, setLastRoadEditJob] = useState<RoadNetworkEditJobResponse | null>(null);
  const completedRoadEditJobIdRef = useRef<number | null>(null);
  const submittedRoadEditAssignmentIdRef = useRef<string | null>(null);
  const {
    page,
    selectedAssignmentId,
    selectedGu,
    selectedDong,
    draftEdits,
    setPage,
    setSelectedArea,
    undoDraftEdit,
    clearDraft,
    requestReview,
    addDraftEdit,
    markApplied,
  } = useAdminStore();

  const hasToken = Boolean(accessToken);
  const isAdminAuthenticated = hasToken && adminPrincipal?.role === "ADMIN";
  const currentAdmin = adminPrincipal;

  useEffect(() => {
    function handleAccessTokenRefreshed(event: Event) {
      const nextToken = (event as CustomEvent<string>).detail;
      if (!nextToken) return;
      setAccessToken(nextToken);
      setTokenInput(nextToken);
    }

    window.addEventListener(adminAccessTokenRefreshedEvent, handleAccessTokenRefreshed);
    return () => window.removeEventListener(adminAccessTokenRefreshedEvent, handleAccessTokenRefreshed);
  }, []);

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
    enabled: page !== "hazards" && isAdminAuthenticated,
    retry: false,
  });

  const payloadQuery = useQuery({
    queryKey: ["admin-road-network", selectedGu, selectedDong, accessToken],
    queryFn: () => fetchAdminRoadNetworkPayload({ gu: selectedGu, dong: selectedDong, accessToken }),
    enabled: page === "network" && isAdminAuthenticated,
    retry: false,
  });

  const facilityQuery = useQuery({
    queryKey: ["admin-facilities", selectedGu, selectedDong, accessToken],
    queryFn: () => fetchAdminFacilityPayload({ gu: selectedGu, dong: selectedDong, accessToken }),
    enabled: page === "facilities" && isAdminAuthenticated,
    retry: false,
  });

  const selectedFacilityPlaceId = selectedFacility ? Number(selectedFacility.properties.placeId) : null;

  const placeDetailQuery = useQuery({
    queryKey: ["admin-place", selectedFacilityPlaceId, accessToken],
    queryFn: () => fetchAdminPlaceDetail(selectedFacilityPlaceId!, accessToken),
    enabled: page === "facilities" && isAdminAuthenticated && Number.isFinite(selectedFacilityPlaceId),
    retry: false,
  });

  const applyRoadNetworkMutation = useMutation({
    mutationFn: () => {
      submittedRoadEditAssignmentIdRef.current = selectedAssignmentId;
      return createAdminRoadNetworkEditJob({
        version: "ADMIN-draft-v1",
        assignmentId: `${selectedGu}:${selectedDong}`,
        gu: selectedGu,
        dong: selectedDong,
        role: currentAdmin?.role ?? "ADMIN",
        createdAt: new Date().toISOString(),
        edits: draftEdits,
      }, accessToken);
    },
    onSuccess: (job) => {
      completedRoadEditJobIdRef.current = null;
      setLastRoadEditJob(job);
      setActiveRoadEditJobId(job.jobId);
    },
  });

  const updatePlaceMutation = useMutation({
    mutationFn: ({ placeId, request }: { placeId: number; request: AdminPlaceUpdateRequest }) =>
      updateAdminPlace(placeId, request, accessToken),
    onSuccess: (place) => {
      queryClient.setQueryData(["admin-place", place.placeId, accessToken], place);
      queryClient.invalidateQueries({ queryKey: ["admin-facilities"] });
    },
  });

  const updatePlaceFeaturesMutation = useMutation({
    mutationFn: ({ placeId, features }: { placeId: number; features: PlaceAccessibilityFeature[] }) =>
      updateAdminPlaceAccessibilityFeatures(placeId, features, accessToken),
    onSuccess: (place) => {
      queryClient.setQueryData(["admin-place", place.placeId, accessToken], place);
      queryClient.invalidateQueries({ queryKey: ["admin-facilities"] });
    },
  });

  const roadEditJobQuery = useQuery({
    queryKey: ["admin-road-network-edit-job", activeRoadEditJobId, accessToken],
    queryFn: () => fetchAdminRoadNetworkEditJob(activeRoadEditJobId!, accessToken),
    enabled: activeRoadEditJobId !== null && isAdminAuthenticated,
    refetchInterval: activeRoadEditJobId === null ? false : 2000,
    retry: false,
  });

  const activeRoadEditJob = roadEditJobQuery.data ?? lastRoadEditJob;
  const isRoadEditJobRunning = activeRoadEditJob?.status === "PENDING" || activeRoadEditJob?.status === "RUNNING";
  const roadEditResult = activeRoadEditJob?.result ?? null;

  useEffect(() => {
    if (!activeRoadEditJob || activeRoadEditJob.status === "PENDING" || activeRoadEditJob.status === "RUNNING") {
      return;
    }
    setLastRoadEditJob(activeRoadEditJob);
    if (activeRoadEditJob.status === "FAILED") {
      setActiveRoadEditJobId(null);
      return;
    }
    if (completedRoadEditJobIdRef.current === activeRoadEditJob.jobId) {
      return;
    }
    completedRoadEditJobIdRef.current = activeRoadEditJob.jobId;
    markApplied(submittedRoadEditAssignmentIdRef.current ?? undefined);
    submittedRoadEditAssignmentIdRef.current = null;
    setSelectedSegment(null);
    setActiveRoadEditJobId(null);
    queryClient.invalidateQueries({ queryKey: ["admin-road-network"] });
    queryClient.invalidateQueries({ queryKey: ["admin-areas"] });
  }, [activeRoadEditJob, markApplied]);

  const filteredDongs = useMemo(() => {
    const areas = areasQuery.data ?? [];
    return areas.filter((area) => area.gu === selectedGu);
  }, [areasQuery.data, selectedGu]);

  function logoutAdmin() {
    void logoutAdminSession(accessToken).catch(() => undefined);
    storeAdminAccessToken("");
    setAccessToken("");
    setTokenInput("");
    setAdminPrincipal(null);
  }

  if (!isAdminAuthenticated || !currentAdmin) {
    return (
      <div className="admin-login-shell">
        <div className="admin-login-panel">
          <span className="login-kicker">BusanEumgil Admin</span>
          <h1>관리자 로그인</h1>
          <p>소셜 로그인 후 관리자 권한이 확인된 계정만 운영 화면에 접근할 수 있습니다.</p>
          <AdminAuthPanel
            accessToken={accessToken}
            tokenInput={tokenInput}
            onAccessTokenChange={setAccessToken}
            onTokenInputChange={setTokenInput}
            onAdminVerified={setAdminPrincipal}
          />
        </div>
      </div>
    );
  }

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
              Admin
              <span>{currentAdmin.userId}</span>
            </label>
            <button type="button" onClick={logoutAdmin}>로그아웃</button>
            <label>
              구
              <select
                value={selectedGu}
                disabled={applyRoadNetworkMutation.isPending || isRoadEditJobRunning}
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
                disabled={applyRoadNetworkMutation.isPending || isRoadEditJobRunning}
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

        {page === "hazards" && <HazardReportsPage accessToken={accessToken} adminPrincipal={currentAdmin} onLogout={logoutAdmin} />}

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
                <button
                  className="primary"
                  onClick={() => applyRoadNetworkMutation.mutate()}
                  disabled={!draftEdits.length || applyRoadNetworkMutation.isPending || isRoadEditJobRunning}
                >
                  {applyRoadNetworkMutation.isPending || isRoadEditJobRunning ? "DB 반영 중" : "DB 반영"}
                </button>
                <button onClick={requestReview} disabled={!draftEdits.length || applyRoadNetworkMutation.isPending || isRoadEditJobRunning}>
                  Request Review
                </button>
                {activeRoadEditJob && (
                  <p className="muted">
                    작업 #{activeRoadEditJob.jobId} {activeRoadEditJob.message}
                    {roadEditResult && (
                      <>
                        {" "}추가 {roadEditResult.addedSegments}, 삭제 {roadEditResult.deletedSegments},
                        생성 node {roadEditResult.createdNodes}, snap {roadEditResult.snappedNodes}
                      </>
                    )}
                  </p>
                )}
                {(applyRoadNetworkMutation.error || roadEditJobQuery.error || activeRoadEditJob?.status === "FAILED") && (
                  <p className="error-box">
                    {applyRoadNetworkMutation.error?.message
                      || roadEditJobQuery.error?.message
                      || activeRoadEditJob?.message}
                  </p>
                )}
                <p className="muted">로컬 draft를 비동기 작업으로 등록한 뒤 DB road_nodes, road_segments, segment_features에 반영합니다.</p>
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
                  <FacilityDetails
                    feature={selectedFacility}
                    detail={placeDetailQuery.data}
                    loading={placeDetailQuery.isLoading}
                    error={placeDetailQuery.error}
                    savingBasic={updatePlaceMutation.isPending}
                    savingFeatures={updatePlaceFeaturesMutation.isPending}
                    saveError={updatePlaceMutation.error || updatePlaceFeaturesMutation.error}
                    onSaveBasic={(placeId, request) => updatePlaceMutation.mutate({ placeId, request })}
                    onSaveFeatures={(placeId, features) => updatePlaceFeaturesMutation.mutate({ placeId, features })}
                  />
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

function FacilityDetails({
  feature,
  detail,
  loading,
  error,
  savingBasic,
  savingFeatures,
  saveError,
  onSaveBasic,
  onSaveFeatures,
}: {
  feature: FacilityFeature;
  detail?: AdminPlaceDetailResponse;
  loading: boolean;
  error?: Error | null;
  savingBasic: boolean;
  savingFeatures: boolean;
  saveError?: Error | null;
  onSaveBasic: (placeId: number, request: AdminPlaceUpdateRequest) => void;
  onSaveFeatures: (placeId: number, features: PlaceAccessibilityFeature[]) => void;
}) {
  const properties = feature.properties;
  const [name, setName] = useState(properties.name || "");
  const [category, setCategory] = useState<PlaceCategory>(properties.category);
  const [address, setAddress] = useState(properties.address || "");
  const [providerPlaceId, setProviderPlaceId] = useState(properties.providerPlaceId || "");
  const [lat, setLat] = useState(String(feature.geometry.coordinates[1] ?? ""));
  const [lng, setLng] = useState(String(feature.geometry.coordinates[0] ?? ""));
  const [features, setFeatures] = useState<Record<AccessibilityFeatureType, boolean>>(() =>
    Object.fromEntries(accessibilityFeatureTypes.map((featureType) => [featureType, false])) as Record<AccessibilityFeatureType, boolean>,
  );

  useEffect(() => {
    if (!detail) return;
    setName(detail.name);
    setCategory(detail.category);
    setAddress(detail.address ?? "");
    setProviderPlaceId(detail.providerPlaceId ?? "");
    setLat(String(detail.point.lat));
    setLng(String(detail.point.lng));
    setFeatures(
      Object.fromEntries(
        accessibilityFeatureTypes.map((featureType) => [
          featureType,
          detail.accessibilityFeatures.some((item) => item.featureType === featureType && item.isAvailable),
        ]),
      ) as Record<AccessibilityFeatureType, boolean>,
    );
  }, [detail]);

  const placeId = Number(properties.placeId);
  const parsedLat = Number(lat);
  const parsedLng = Number(lng);
  const canSave = Number.isFinite(placeId) && Boolean(detail) && Number.isFinite(parsedLat) && Number.isFinite(parsedLng);

  function saveBasic() {
    if (!canSave) return;
    onSaveBasic(placeId, {
      name,
      category,
      address,
      providerPlaceId,
      point: {
        lat: parsedLat,
        lng: parsedLng,
      },
    });
  }

  function saveFeatures() {
    if (!canSave) return;
    onSaveFeatures(
      placeId,
      accessibilityFeatureTypes.map((featureType) => ({
        featureType,
        isAvailable: features[featureType],
      })),
    );
  }

  return (
    <>
      <dl className="attribute-detail-list">
        <AttributeRow label="place" value={properties.placeId} />
        <AttributeRow label="provider" value={properties.providerPlaceId || "-"} />
        <AttributeRow label="이름" value={properties.name || "-"} />
        <AttributeRow label="분류" value={facilityCategoryLabel(properties.category)} />
        <AttributeRow label="주소" value={properties.address || "-"} />
      </dl>
      {loading && <p className="muted">상세 정보를 불러오는 중입니다.</p>}
      {error && <p className="error-box">{error.message}</p>}
      {saveError && <p className="error-box">{saveError.message}</p>}
      {detail && (
        <>
          <div className="admin-form-grid">
            <label>
              이름
              <input value={name} onChange={(event) => setName(event.target.value)} />
            </label>
            <label>
              카테고리
              <select value={category} onChange={(event) => setCategory(event.target.value as PlaceCategory)}>
                {placeCategories.map((item) => (
                  <option key={item} value={item}>
                    {facilityCategoryLabel(item)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              주소
              <input value={address} onChange={(event) => setAddress(event.target.value)} />
            </label>
            <label>
              providerPlaceId
              <input value={providerPlaceId} onChange={(event) => setProviderPlaceId(event.target.value)} />
            </label>
            <label>
              lat
              <input value={lat} onChange={(event) => setLat(event.target.value)} />
            </label>
            <label>
              lng
              <input value={lng} onChange={(event) => setLng(event.target.value)} />
            </label>
          </div>
          <div className="button-row">
            <button className="primary" type="button" onClick={saveBasic} disabled={savingBasic || !canSave}>
              {savingBasic ? "저장 중" : "기본 정보 저장"}
            </button>
          </div>
          <div className="feature-toggle-list">
            {accessibilityFeatureTypes.map((featureType) => (
              <label key={featureType}>
                <input
                  type="checkbox"
                  checked={features[featureType]}
                  onChange={(event) => setFeatures((value) => ({ ...value, [featureType]: event.target.checked }))}
                />
                {accessibilityFeatureLabel(featureType)}
              </label>
            ))}
          </div>
          <div className="button-row">
            <button className="primary" type="button" onClick={saveFeatures} disabled={savingFeatures || !canSave}>
              {savingFeatures ? "저장 중" : "접근성 저장"}
            </button>
          </div>
        </>
      )}
    </>
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

function accessibilityFeatureLabel(featureType: AccessibilityFeatureType) {
  switch (featureType) {
    case "accessibleEntrance":
      return "단차 없는 출입";
    case "elevator":
      return "엘리베이터";
    case "accessibleToilet":
      return "장애인 화장실";
    case "accessibleParking":
      return "장애인 주차";
    case "chargingStation":
      return "전동보장구 충전";
    case "accessibleRoom":
      return "객실 이용";
    case "guidanceFacility":
      return "안내시설";
  }
}

function formatNumber(value?: number | null) {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  return value.toFixed(value % 1 === 0 ? 0 : 1);
}

export default App;
