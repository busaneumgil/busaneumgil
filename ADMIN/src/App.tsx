import { useEffect, useMemo, useRef, useState } from "react";
import { QueryClient, QueryClientProvider, useInfiniteQuery, useMutation, useQuery } from "@tanstack/react-query";
import {
  createAdminRoadNetworkEditJob,
  fetchAdminAreaAssignments,
  fetchAdminAuditLogs,
  fetchAdminAreas,
  fetchAdminFacilityPayload,
  fetchAdminPlaceDetail,
  fetchAdminRoadNetworkPayload,
  fetchAdminRoadNetworkEditJob,
  fetchAdminHazardReports,
  fetchAdminUsers,
  adminAccessTokenRefreshedEvent,
  getStoredAdminAccessToken,
  logoutAdminSession,
  reverseGeocodePlace,
  storeAdminAccessToken,
  updateAdminAreaAssignmentStatus,
  updateAdminPlace,
  updateAdminPlaceAccessibilityFeatures,
  updateAdminUserRole,
  upsertAdminAreaAssignment,
} from "./api/adminApi";
import { AdminAuthPanel } from "./auth/AdminAuthPanel";
import { adminShellClassName } from "./layout/adminLayout";
import { FacilityMap } from "./map/FacilityMap";
import { facilityCategoryLabel } from "./map/facilityStyle";
import { SegmentMap, type RoadviewDockState } from "./map/SegmentMap";
import { HazardReportsPage } from "./report/HazardReportsPage";
import { RouteTuningPage } from "./route/RouteTuningPage";
import { useAdminStore } from "./store/adminStore";
import type {
  AccessibilityFeatureType,
  AdminAuditLog,
  AdminMeResponse,
  AdminPage,
  AdminPlaceDetailResponse,
  AdminPlaceUpdateRequest,
  FacilityFeature,
  PlaceAccessibilityFeature,
  PlaceCategory,
  RoadNetworkEditJobResponse,
  SegmentFeature,
  WorkStatus,
  AdminUserResponse,
  Assignment,
  AssignmentType,
  GeoPoint,
  UserRole,
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

const auditLogActions = [
  { value: "ROAD_NETWORK_EDIT_APPLY", label: "보행 네트워크 반영" },
  { value: "ROAD_SEGMENT_ATTRIBUTES_UPDATE", label: "segment 속성 변경" },
  { value: "PLACE_BASIC_UPDATE", label: "편의시설 기본 정보 변경" },
  { value: "PLACE_ACCESSIBILITY_FEATURES_REPLACE", label: "편의시설 접근성 변경" },
];

const pageMeta: Record<AdminPage, { label: string; description: string }> = {
  network: {
    label: "보행 네트워크",
    description: "SIDE_LINE/CROSS_WALK를 구·동 단위로 편집하고 DB 반영 전 draft를 검수합니다.",
  },
  routeTuning: {
    label: "경로 검수",
    description: "구·동별 보행 네트워크 속성을 조정하고 프로필별 안전/빠른 경로를 비교합니다.",
  },
  facilities: {
    label: "편의시설",
    description: "보행약자 편의시설 위치와 접근성 속성을 검수합니다.",
  },
  hazards: {
    label: "제보 관리",
    description: "사용자가 등록한 도로 상태 제보를 확인하고 승인 또는 반려합니다.",
  },
  users: {
    label: "사용자 관리",
    description: "관리자 권한과 구·동 담당자, 작업 상태를 관리합니다.",
  },
  logs: {
    label: "변경 로그",
    description: "관리자 화면에서 수행한 변경 작업을 최신순으로 확인합니다.",
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
  const [facilityLocationPickEnabled, setFacilityLocationPickEnabled] = useState(false);
  const [facilityPickedLocation, setFacilityPickedLocation] = useState<{ point: GeoPoint; address?: string; nonce: number } | null>(null);
  const [accessToken, setAccessToken] = useState(getStoredAdminAccessToken);
  const [tokenInput, setTokenInput] = useState(accessToken);
  const [adminPrincipal, setAdminPrincipal] = useState<AdminMeResponse | null>(null);
  const [activeRoadEditJobId, setActiveRoadEditJobId] = useState<number | null>(null);
  const [lastRoadEditJob, setLastRoadEditJob] = useState<RoadNetworkEditJobResponse | null>(null);
  const [auditLogAction, setAuditLogAction] = useState("");
  const [auditLogGu, setAuditLogGu] = useState("");
  const [auditLogDong, setAuditLogDong] = useState("");
  const [auditLogActorUserId, setAuditLogActorUserId] = useState("");
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
    addDraftEdit,
    clearDraftForAssignment,
  } = useAdminStore();

  const hasToken = Boolean(accessToken);
  const isAdminAuthenticated = hasToken && adminPrincipal?.role === "ADMIN";
  const currentAdmin = adminPrincipal;
  const showsAreaSelector = page === "network" || page === "facilities" || page === "routeTuning";
  const selectedAssignmentType: AssignmentType = page === "facilities" ? "FACILITY" : "ROAD_NETWORK";

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
    enabled: (showsAreaSelector || page === "users" || page === "logs") && isAdminAuthenticated,
    retry: false,
  });

  const adminUsersQuery = useQuery({
    queryKey: ["admin-users", accessToken],
    queryFn: () => fetchAdminUsers(accessToken),
    enabled: (page === "users" || page === "logs") && isAdminAuthenticated,
    retry: false,
  });

  const areaAssignmentsQuery = useQuery({
    queryKey: ["admin-area-assignments", accessToken],
    queryFn: () => fetchAdminAreaAssignments(accessToken),
    enabled: isAdminAuthenticated,
    retry: false,
  });

  const pendingHazardReportsQuery = useQuery({
    queryKey: ["admin-hazard-reports-pending-count", accessToken],
    queryFn: () => fetchAdminHazardReports({ status: "PENDING", cursor: null, size: 20, accessToken }),
    enabled: isAdminAuthenticated,
    retry: false,
    refetchInterval: 30_000,
  });

  const auditLogsQuery = useInfiniteQuery({
    queryKey: ["admin-audit-logs", accessToken, auditLogAction, auditLogGu, auditLogDong, auditLogActorUserId],
    queryFn: ({ pageParam }) => fetchAdminAuditLogs({
      action: auditLogAction,
      gu: auditLogGu,
      dong: auditLogDong,
      actorUserId: auditLogActorUserId,
      cursor: pageParam,
      size: 50,
      accessToken,
    }),
    initialPageParam: null as number | null,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.nextCursor : undefined),
    enabled: page === "logs" && isAdminAuthenticated,
    retry: false,
  });
  const auditLogs = auditLogsQuery.data?.pages.flatMap((logPage) => logPage.logs) ?? [];

  const payloadQuery = useQuery({
    queryKey: ["admin-road-network", selectedGu, selectedDong, accessToken],
    queryFn: () => fetchAdminRoadNetworkPayload({ gu: selectedGu, dong: selectedDong, accessToken }),
    enabled: (page === "network" || page === "routeTuning") && isAdminAuthenticated,
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

  const updateUserRoleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: UserRole }) =>
      updateAdminUserRole(userId, role, accessToken),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
    },
  });

  const upsertAssignmentMutation = useMutation({
    mutationFn: (request: { gu: string; dong: string; assignmentType: AssignmentType; assigneeUserId: string | null; status: WorkStatus }) =>
      upsertAdminAreaAssignment(request, accessToken),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-area-assignments"] });
    },
  });

  const updateAssignmentStatusMutation = useMutation({
    mutationFn: ({ assignmentId, status }: { assignmentId: number; status: WorkStatus }) =>
      updateAdminAreaAssignmentStatus(assignmentId, status, accessToken),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-area-assignments"] });
    },
  });

  const updatePlaceMutation = useMutation({
    mutationFn: ({ placeId, request }: { placeId: number; request: AdminPlaceUpdateRequest }) =>
      updateAdminPlace(placeId, selectedGu, selectedDong, request, accessToken),
    onSuccess: (place) => {
      queryClient.setQueryData(["admin-place", place.placeId, accessToken], place);
      queryClient.invalidateQueries({ queryKey: ["admin-facilities"] });
    },
  });

  const updatePlaceFeaturesMutation = useMutation({
    mutationFn: ({ placeId, features }: { placeId: number; features: PlaceAccessibilityFeature[] }) =>
      updateAdminPlaceAccessibilityFeatures(placeId, selectedGu, selectedDong, features, accessToken),
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
    clearDraftForAssignment(submittedRoadEditAssignmentIdRef.current ?? undefined);
    submittedRoadEditAssignmentIdRef.current = null;
    setSelectedSegment(null);
    setActiveRoadEditJobId(null);
    queryClient.invalidateQueries({ queryKey: ["admin-road-network"] });
    queryClient.invalidateQueries({ queryKey: ["admin-areas"] });
    queryClient.invalidateQueries({ queryKey: ["admin-area-assignments"] });
  }, [activeRoadEditJob, clearDraftForAssignment]);

  const filteredDongs = useMemo(() => {
    const areas = areasQuery.data ?? [];
    return areas.filter((area) => area.gu === selectedGu);
  }, [areasQuery.data, selectedGu]);

  const auditLogGuOptions = useMemo(() => {
    return Array.from(new Set((areasQuery.data ?? []).map((area) => area.gu))).sort();
  }, [areasQuery.data]);

  const auditLogDongOptions = useMemo(() => {
    const areas = areasQuery.data ?? [];
    return areas
      .filter((area) => !auditLogGu || area.gu === auditLogGu)
      .map((area) => area.dong)
      .filter((dong, index, dongs) => dongs.indexOf(dong) === index)
      .sort();
  }, [areasQuery.data, auditLogGu]);

  const selectedAssignment = useMemo(() => {
    return (areaAssignmentsQuery.data ?? []).find((assignment) =>
      assignment.gu === selectedGu
      && assignment.dong === selectedDong
      && assignment.assignmentType === selectedAssignmentType) ?? null;
  }, [areaAssignmentsQuery.data, selectedAssignmentType, selectedDong, selectedGu]);

  const canEditSelectedArea = selectedAssignment?.assigneeUserId === currentAdmin?.userId;
  const selectedAssignmentLabel = selectedAssignment?.assigneeLabel || selectedAssignment?.assigneeUserId || "미지정";
  const pendingHazardCount = pendingHazardReportsQuery.data?.content.length ?? 0;
  const pendingHazardBadge = pendingHazardReportsQuery.data?.hasNext
    ? `${pendingHazardCount}+`
    : String(pendingHazardCount);

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
              {item === "hazards" && pendingHazardCount > 0 && (
                <span className="nav-badge" aria-label={`대기 제보 ${pendingHazardBadge}건`}>
                  {pendingHazardBadge}
                </span>
              )}
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
          {page !== "hazards" && page !== "users" && <div className="topbar-actions">
            <label className="backend-field">
              Admin
              <span>{currentAdmin.userId}</span>
            </label>
            <button type="button" onClick={logoutAdmin}>로그아웃</button>
            {showsAreaSelector && (
              <>
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
              </>
            )}
          </div>}
        </header>

        {page === "hazards" && <HazardReportsPage accessToken={accessToken} adminPrincipal={currentAdmin} onLogout={logoutAdmin} />}

        {page === "routeTuning" && (
          <RouteTuningPage
            accessToken={accessToken}
            gu={selectedGu}
            dong={selectedDong}
            payload={payloadQuery.data}
            loading={payloadQuery.isLoading}
            error={payloadQuery.error}
            selectedSegment={selectedSegment}
            onSelectSegment={setSelectedSegment}
            canEdit={canEditSelectedArea}
            assignmentMessage={
              canEditSelectedArea
                ? `${selectedGu} ${selectedDong} 보행 네트워크 담당자로 수정할 수 있습니다.`
                : `${selectedGu} ${selectedDong} 보행 네트워크 담당자만 수정할 수 있습니다. 현재 담당자: ${selectedAssignmentLabel}`
            }
            roadviewContainerRef={roadviewContainerRef}
            onRoadviewChange={setRoadviewDock}
            onSegmentUpdated={() => {
              queryClient.invalidateQueries({ queryKey: ["admin-road-network"] });
            }}
          />
        )}

        {page === "users" && (
          <UserManagementPage
            currentAdmin={currentAdmin}
            users={adminUsersQuery.data ?? []}
            assignments={areaAssignmentsQuery.data ?? []}
            areas={areasQuery.data ?? []}
            loading={adminUsersQuery.isLoading || areaAssignmentsQuery.isLoading}
            error={adminUsersQuery.error || areaAssignmentsQuery.error}
            userRolePending={updateUserRoleMutation.isPending}
            assignmentPending={upsertAssignmentMutation.isPending || updateAssignmentStatusMutation.isPending}
            onUpdateUserRole={(userId, role) => updateUserRoleMutation.mutate({ userId, role })}
            onUpsertAssignment={(request) => upsertAssignmentMutation.mutate(request)}
            onUpdateAssignmentStatus={(assignmentId, status) => updateAssignmentStatusMutation.mutate({ assignmentId, status })}
          />
        )}

        {page === "logs" && (
          <AuditLogsPage
            logs={auditLogs}
            action={auditLogAction}
            gu={auditLogGu}
            dong={auditLogDong}
            actorUserId={auditLogActorUserId}
            guOptions={auditLogGuOptions}
            dongOptions={auditLogDongOptions}
            users={adminUsersQuery.data ?? []}
            loading={auditLogsQuery.isLoading}
            loadingMore={auditLogsQuery.isFetchingNextPage}
            hasNext={Boolean(auditLogsQuery.hasNextPage)}
            error={auditLogsQuery.error}
            onActionChange={setAuditLogAction}
            onGuChange={(gu) => {
              setAuditLogGu(gu);
              setAuditLogDong("");
            }}
            onDongChange={setAuditLogDong}
            onActorUserIdChange={setAuditLogActorUserId}
            onRefresh={() => void auditLogsQuery.refetch()}
            onLoadMore={() => void auditLogsQuery.fetchNextPage()}
          />
        )}

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
              editable={canEditSelectedArea}
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
                <h3>편집 기준</h3>
                <p className="muted">
                  보행 네트워크 탭은 segment 추가, 삭제, DB 반영에만 사용합니다. 통행, 계단, 보도 폭 같은 segment 속성 검수는 경로 검수 탭에서 진행합니다.
                </p>
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
                <dl className="attribute-detail-list">
                  <AttributeRow label="담당자" value={selectedAssignmentLabel} />
                  <AttributeRow label="상태" value={workStatusLabel(selectedAssignment?.status ?? "NOT_STARTED")} />
                </dl>
                {!canEditSelectedArea && (
                  <p className="error-box">현재 계정은 {selectedGu} {selectedDong} 담당자가 아니므로 수정할 수 없습니다.</p>
                )}
                <button
                  className="primary"
                  onClick={() => applyRoadNetworkMutation.mutate()}
                  disabled={!draftEdits.length || !canEditSelectedArea || applyRoadNetworkMutation.isPending || isRoadEditJobRunning}
                >
                  {applyRoadNetworkMutation.isPending || isRoadEditJobRunning ? "DB 반영 중" : "DB 반영"}
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
              locationPickEnabled={facilityLocationPickEnabled}
              onPickLocation={async (point) => {
                try {
                  const geocode = await reverseGeocodePlace(point, accessToken);
                  setFacilityPickedLocation({
                    point,
                    address: geocode.displayAddress ?? geocode.roadAddress ?? geocode.address ?? undefined,
                    nonce: Date.now(),
                  });
                } catch {
                  setFacilityPickedLocation({ point, nonce: Date.now() });
                }
                setFacilityLocationPickEnabled(false);
              }}
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
                    editable={canEditSelectedArea}
                    pickedLocation={facilityPickedLocation}
                    locationPickEnabled={facilityLocationPickEnabled}
                    assignmentMessage={
                      canEditSelectedArea
                        ? `${selectedGu} ${selectedDong} 편의시설 담당자로 수정할 수 있습니다.`
                        : `${selectedGu} ${selectedDong} 편의시설 담당자만 수정할 수 있습니다. 현재 담당자: ${selectedAssignmentLabel}`
                    }
                    onToggleLocationPick={() => setFacilityLocationPickEnabled((value) => !value)}
                    onSaveBasic={(placeId, request) => updatePlaceMutation.mutate({ placeId, request })}
                    onSaveFeatures={(placeId, features) => updatePlaceFeaturesMutation.mutate({ placeId, features })}
                  />
                ) : (
                  <p className="muted">지도에서 편의시설 점을 hover하면 요약을 보고, 클릭하면 상세와 Roadview를 고정합니다.</p>
                )}
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

function UserManagementPage({
  currentAdmin,
  users,
  assignments,
  areas,
  loading,
  error,
  userRolePending,
  assignmentPending,
  onUpdateUserRole,
  onUpsertAssignment,
  onUpdateAssignmentStatus,
}: {
  currentAdmin: AdminMeResponse;
  users: AdminUserResponse[];
  assignments: Assignment[];
  areas: { gu: string; dong: string }[];
  loading: boolean;
  error?: Error | null;
  userRolePending: boolean;
  assignmentPending: boolean;
  onUpdateUserRole: (userId: string, role: UserRole) => void;
  onUpsertAssignment: (request: { gu: string; dong: string; assignmentType: AssignmentType; assigneeUserId: string | null; status: WorkStatus }) => void;
  onUpdateAssignmentStatus: (assignmentId: number, status: WorkStatus) => void;
}) {
  const adminUsers = users.filter((user) => user.role === "ADMIN").sort((left, right) => {
    if (left.userId === currentAdmin.userId) return -1;
    if (right.userId === currentAdmin.userId) return 1;
    return left.userId.localeCompare(right.userId);
  });
  const assignmentByArea = new Map(assignments.map((assignment) => [`${assignment.gu}:${assignment.dong}:${assignment.assignmentType}`, assignment]));
  const normalizedAreas = areas.length
    ? areas
    : [...new Map(assignments.map((assignment) => [`${assignment.gu}:${assignment.dong}`, { gu: assignment.gu, dong: assignment.dong }])).values()];
  const guOptions = [...new Set(normalizedAreas.map((area) => area.gu))].filter(Boolean).sort((left, right) => left.localeCompare(right, "ko"));
  const [promoteUserId, setPromoteUserId] = useState("");
  const [selectedGuFilter, setSelectedGuFilter] = useState("");
  const guOptionsKey = guOptions.join("|");
  useEffect(() => {
    if (!guOptions.length) {
      if (selectedGuFilter) setSelectedGuFilter("");
      return;
    }
    if (!selectedGuFilter || !guOptions.includes(selectedGuFilter)) {
      setSelectedGuFilter(guOptions[0]);
    }
  }, [guOptionsKey, selectedGuFilter]);
  const filteredAreas = selectedGuFilter
    ? normalizedAreas.filter((area) => area.gu === selectedGuFilter)
    : normalizedAreas;

  return (
    <div className="user-management-layout">
      <section className="panel-section">
        <h3>관리자 권한</h3>
        <form
          className="admin-promote-form"
          onSubmit={(event) => {
            event.preventDefault();
            const userId = promoteUserId.trim();
            if (!userId) return;
            onUpdateUserRole(userId, "ADMIN");
            setPromoteUserId("");
          }}
        >
          <label>
            userId로 관리자 추가
            <input
              value={promoteUserId}
              placeholder="UUID"
              onChange={(event) => setPromoteUserId(event.target.value)}
            />
          </label>
          <button type="submit" disabled={userRolePending || !promoteUserId.trim()}>
            ADMIN 승격
          </button>
        </form>
        {loading && <p className="muted">사용자 정보를 불러오는 중입니다.</p>}
        {error && <p className="error-box">{error.message}</p>}
        <div className="admin-table-scroll">
          <table className="admin-table">
            <thead>
              <tr>
                <th>사용자</th>
                <th>소셜</th>
                <th>권한</th>
                <th>변경</th>
              </tr>
            </thead>
            <tbody>
              {adminUsers.map((user) => (
                <tr key={user.userId}>
                  <td>
                    <strong>{adminUserLabel(user)}</strong>
                    <span>{user.userId}</span>
                  </td>
                  <td>{user.socialProvider} / {user.socialProviderUserId}</td>
                  <td>{user.role}</td>
                  <td>
                    <select
                      value={user.role}
                      disabled={userRolePending || user.userId === currentAdmin.userId}
                      onChange={(event) => onUpdateUserRole(user.userId, event.target.value as UserRole)}
                    >
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </td>
                </tr>
              ))}
              {!adminUsers.length && (
                <tr>
                  <td colSpan={4}>관리자가 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel-section">
        <h3>구·동 담당자 및 작업 상태</h3>
        <p className="muted">보행 네트워크와 편의시설 담당자를 분리합니다. 담당자로 지정된 관리자만 해당 영역을 수정할 수 있습니다.</p>
        <div className="assignment-filter-row">
          <label>
            구
            <select value={selectedGuFilter} onChange={(event) => setSelectedGuFilter(event.target.value)}>
              {guOptions.map((gu) => (
                <option key={gu} value={gu}>{gu}</option>
              ))}
            </select>
          </label>
        </div>
        <AssignmentTable
          title="보행 네트워크 담당 현황"
          assignmentType="ROAD_NETWORK"
          normalizedAreas={filteredAreas}
          assignmentByArea={assignmentByArea}
          adminUsers={adminUsers}
          assignmentPending={assignmentPending}
          onUpsertAssignment={onUpsertAssignment}
          onUpdateAssignmentStatus={onUpdateAssignmentStatus}
        />
        <AssignmentTable
          title="편의시설 담당 현황"
          assignmentType="FACILITY"
          normalizedAreas={filteredAreas}
          assignmentByArea={assignmentByArea}
          adminUsers={adminUsers}
          assignmentPending={assignmentPending}
          onUpsertAssignment={onUpsertAssignment}
          onUpdateAssignmentStatus={onUpdateAssignmentStatus}
        />
      </section>
    </div>
  );
}

function AssignmentTable({
  title,
  assignmentType,
  normalizedAreas,
  assignmentByArea,
  adminUsers,
  assignmentPending,
  onUpsertAssignment,
  onUpdateAssignmentStatus,
}: {
  title: string;
  assignmentType: AssignmentType;
  normalizedAreas: { gu: string; dong: string }[];
  assignmentByArea: Map<string, Assignment>;
  adminUsers: AdminUserResponse[];
  assignmentPending: boolean;
  onUpsertAssignment: (request: { gu: string; dong: string; assignmentType: AssignmentType; assigneeUserId: string | null; status: WorkStatus }) => void;
  onUpdateAssignmentStatus: (assignmentId: number, status: WorkStatus) => void;
}) {
  return (
    <>
      <h4>{title}</h4>
      <div className="admin-table-scroll">
        <table className="admin-table">
          <thead>
            <tr>
              <th>구</th>
              <th>동</th>
              <th>담당자</th>
              <th>상태</th>
              <th>수정일</th>
            </tr>
          </thead>
          <tbody>
            {normalizedAreas.map((area) => {
              const assignment = assignmentByArea.get(`${area.gu}:${area.dong}:${assignmentType}`);
              const status = assignment?.status ?? "NOT_STARTED";
              return (
                <tr key={`${assignmentType}:${area.gu}:${area.dong}`}>
                  <td>{area.gu}</td>
                  <td>{area.dong}</td>
                  <td>
                    <select
                      value={assignment?.assigneeUserId ?? ""}
                      disabled={assignmentPending}
                      onChange={(event) =>
                        onUpsertAssignment({
                          gu: area.gu,
                          dong: area.dong,
                          assignmentType,
                          assigneeUserId: event.target.value || null,
                          status,
                        })
                      }
                    >
                      <option value="">미지정</option>
                      {adminUsers.map((user) => (
                        <option key={user.userId} value={user.userId}>
                          {adminUserLabel(user)}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <select
                      value={status}
                      disabled={assignmentPending}
                      onChange={(event) => {
                        const nextStatus = event.target.value as WorkStatus;
                        if (assignment?.assignmentId) {
                          onUpdateAssignmentStatus(assignment.assignmentId, nextStatus);
                          return;
                        }
                        onUpsertAssignment({
                          gu: area.gu,
                          dong: area.dong,
                          assignmentType,
                          assigneeUserId: assignment?.assigneeUserId ?? null,
                          status: nextStatus,
                        });
                      }}
                    >
                      {workStatusOptions.map((item) => (
                        <option key={item} value={item}>
                          {workStatusLabel(item)}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>{assignment?.updatedAt ? formatDateTime(assignment.updatedAt) : "-"}</td>
                </tr>
              );
            })}
            {!normalizedAreas.length && (
              <tr>
                <td colSpan={5}>구·동 목록이 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function AuditLogsPage({
  logs,
  action,
  gu,
  dong,
  actorUserId,
  guOptions,
  dongOptions,
  users,
  loading,
  loadingMore,
  hasNext,
  error,
  onActionChange,
  onGuChange,
  onDongChange,
  onActorUserIdChange,
  onRefresh,
  onLoadMore,
}: {
  logs: AdminAuditLog[];
  action: string;
  gu: string;
  dong: string;
  actorUserId: string;
  guOptions: string[];
  dongOptions: string[];
  users: AdminUserResponse[];
  loading: boolean;
  loadingMore: boolean;
  hasNext: boolean;
  error?: Error | null;
  onActionChange: (action: string) => void;
  onGuChange: (gu: string) => void;
  onDongChange: (dong: string) => void;
  onActorUserIdChange: (userId: string) => void;
  onRefresh: () => void;
  onLoadMore: () => void;
}) {
  return (
    <section className="audit-log-page">
      <div className="panel-toolbar">
        <div>
          <h3>변경 로그</h3>
          <p className="muted">관리자 화면에서 성공적으로 반영된 변경 작업만 기록됩니다.</p>
        </div>
        <button type="button" onClick={onRefresh} disabled={loading}>
          새로고침
        </button>
      </div>
      <div className="audit-log-filters">
        <label>
          작업 종류
          <select value={action} onChange={(event) => onActionChange(event.target.value)}>
            <option value="">전체</option>
            {auditLogActions.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          구
          <select value={gu} onChange={(event) => onGuChange(event.target.value)}>
            <option value="">전체</option>
            {guOptions.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>
        <label>
          동
          <select value={dong} onChange={(event) => onDongChange(event.target.value)}>
            <option value="">전체</option>
            {dongOptions.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>
        <label>
          작업자
          <select value={actorUserId} onChange={(event) => onActorUserIdChange(event.target.value)}>
            <option value="">전체</option>
            {users.map((user) => (
              <option key={user.userId} value={user.userId}>
                {adminUserLabel(user)}
              </option>
            ))}
          </select>
        </label>
      </div>
      {loading && <p className="muted">변경 로그를 불러오는 중입니다.</p>}
      {error && <p className="error-box">{error.message}</p>}
      <div className="audit-log-list">
        {logs.map((log) => (
          <article key={log.logId} className="audit-log-card">
            <header>
              <strong>{adminAuditActionLabel(log.action)}</strong>
              <time>{formatDateTime(log.createdAt)}</time>
            </header>
            <p>{log.summary}</p>
            <dl className="audit-log-meta">
              <div>
                <dt>작업자</dt>
                <dd title={log.actorUserId}>{shortId(log.actorUserId)}</dd>
              </div>
              <div>
                <dt>대상</dt>
                <dd>{log.targetType}{log.targetId ? ` #${log.targetId}` : ""}</dd>
              </div>
              <div>
                <dt>구/동</dt>
                <dd>{log.gu && log.dong ? `${log.gu} ${log.dong}` : "-"}</dd>
              </div>
            </dl>
            {Boolean(log.beforeJson || log.afterJson) && (
              <details className="audit-log-json">
                <summary>변경 전/후 보기</summary>
                <div>
                  <pre>{formatJson(log.beforeJson)}</pre>
                  <pre>{formatJson(log.afterJson)}</pre>
                </div>
              </details>
            )}
          </article>
        ))}
        {!loading && !logs.length && <p className="muted">표시할 변경 로그가 없습니다.</p>}
      </div>
      {hasNext && (
        <button
          className="audit-log-more-button"
          type="button"
          onClick={onLoadMore}
          disabled={loadingMore}
        >
          {loadingMore ? "불러오는 중..." : "더 보기"}
        </button>
      )}
    </section>
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

const workStatusOptions: WorkStatus[] = ["NOT_STARTED", "IN_PROGRESS", "COMPLETED", "HOLD"];

function workStatusLabel(status: WorkStatus) {
  switch (status) {
    case "NOT_STARTED":
      return "미시작";
    case "IN_PROGRESS":
      return "진행중";
    case "COMPLETED":
      return "완료";
    case "HOLD":
      return "보류";
  }
}

function shortId(userId: string) {
  return userId.length > 8 ? userId.slice(0, 8) : userId;
}

function adminUserLabel(user: AdminUserResponse) {
  return shortId(user.userId);
}

function adminAuditActionLabel(action: string) {
  switch (action) {
    case "ROAD_NETWORK_EDIT_APPLY":
      return "보행 네트워크 반영";
    case "ROAD_SEGMENT_ATTRIBUTES_UPDATE":
      return "segment 속성 변경";
    case "PLACE_BASIC_UPDATE":
      return "편의시설 기본 정보 변경";
    case "PLACE_ACCESSIBILITY_FEATURES_REPLACE":
      return "편의시설 접근성 변경";
    default:
      return action;
  }
}

function formatJson(value: unknown) {
  if (value == null) {
    return "-";
  }
  return JSON.stringify(value, null, 2);
}

function formatDateTime(value: string) {
  return value.replace("T", " ").slice(0, 16);
}

function FacilityDetails({
  feature,
  detail,
  loading,
  error,
  savingBasic,
  savingFeatures,
  saveError,
  editable,
  pickedLocation,
  locationPickEnabled,
  assignmentMessage,
  onToggleLocationPick,
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
  editable: boolean;
  pickedLocation: { point: GeoPoint; address?: string; nonce: number } | null;
  locationPickEnabled: boolean;
  assignmentMessage: string;
  onToggleLocationPick: () => void;
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

  useEffect(() => {
    if (!pickedLocation) return;
    setLat(String(pickedLocation.point.lat));
    setLng(String(pickedLocation.point.lng));
    if (pickedLocation.address) {
      setAddress(pickedLocation.address);
    }
  }, [pickedLocation]);

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
      <p className={editable ? "muted" : "error-box"}>{assignmentMessage}</p>
      {detail && (
        <>
          <div className="admin-form-grid">
            <label>
              이름
              <input value={name} disabled={!editable} onChange={(event) => setName(event.target.value)} />
            </label>
            <label>
              카테고리
              <select value={category} disabled={!editable} onChange={(event) => setCategory(event.target.value as PlaceCategory)}>
                {placeCategories.map((item) => (
                  <option key={item} value={item}>
                    {facilityCategoryLabel(item)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              주소
              <input value={address} disabled={!editable} onChange={(event) => setAddress(event.target.value)} />
            </label>
            <label>
              providerPlaceId
              <input value={providerPlaceId} disabled={!editable} onChange={(event) => setProviderPlaceId(event.target.value)} />
            </label>
            <label>
              lat
              <input value={lat} disabled={!editable} onChange={(event) => setLat(event.target.value)} />
            </label>
            <label>
              lng
              <input value={lng} disabled={!editable} onChange={(event) => setLng(event.target.value)} />
            </label>
          </div>
          <div className="button-row">
            <button type="button" onClick={onToggleLocationPick} disabled={!editable}>
              {locationPickEnabled ? "지도 클릭 대기 중" : "지도 클릭으로 위치 선택"}
            </button>
            <button className="primary" type="button" onClick={saveBasic} disabled={savingBasic || !canSave || !editable}>
              {savingBasic ? "저장 중" : "기본 정보 저장"}
            </button>
          </div>
          <div className="feature-toggle-list">
            {accessibilityFeatureTypes.map((featureType) => (
              <label key={featureType}>
                <input
                  type="checkbox"
                  disabled={!editable}
                  checked={features[featureType]}
                  onChange={(event) => setFeatures((value) => ({ ...value, [featureType]: event.target.checked }))}
                />
                {accessibilityFeatureLabel(featureType)}
              </label>
            ))}
          </div>
          <div className="button-row">
            <button className="primary" type="button" onClick={saveFeatures} disabled={savingFeatures || !canSave || !editable}>
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
