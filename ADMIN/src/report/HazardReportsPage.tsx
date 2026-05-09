import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  approveAdminHazardReport,
  fetchAdminHazardReportDetail,
  fetchAdminHazardReports,
  rejectAdminHazardReport,
} from "../api/adminApi";
import type {
  AdminHazardReportDetail,
  AdminHazardReportSummary,
  AdminMeResponse,
  HazardReportStatus,
  HazardReportType,
} from "../types";

const reportStatusOptions: Array<"" | HazardReportStatus> = ["", "PENDING", "APPROVED", "REJECTED"];

const reportStatusLabel: Record<HazardReportStatus, string> = {
  PENDING: "대기",
  APPROVED: "승인",
  REJECTED: "반려",
};

const reportTypeLabel: Record<HazardReportType, string> = {
  STAIRS_STEP: "계단/단차",
  BRAILLE_BLOCK: "점자블록",
  SIDEWALK_MISSING: "보도 없음",
  RAMP: "경사로",
  SIDEWALK_WIDTH: "보도 폭",
  OTHER_OBSTACLE: "기타 장애물",
};

interface HazardReportsPageProps {
  accessToken: string;
  adminPrincipal: AdminMeResponse;
  onLogout: () => void;
}

export function HazardReportsPage({ accessToken, adminPrincipal, onLogout }: HazardReportsPageProps) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<"" | HazardReportStatus>("PENDING");
  const [cursorStack, setCursorStack] = useState<Array<number | null>>([null]);
  const [selectedReportId, setSelectedReportId] = useState<number | null>(null);
  const cursor = cursorStack[cursorStack.length - 1] ?? null;
  const hasToken = Boolean(accessToken);

  const reportsQuery = useQuery({
    queryKey: ["admin-hazard-reports", status, cursor, accessToken],
    queryFn: () => fetchAdminHazardReports({ status, cursor, size: 10, accessToken }),
    enabled: hasToken,
    retry: false,
  });

  const selectedReport = useMemo(
    () => reportsQuery.data?.content.find((report) => report.reportId === selectedReportId) ?? null,
    [reportsQuery.data, selectedReportId],
  );

  const detailQuery = useQuery({
    queryKey: ["admin-hazard-report-detail", selectedReportId, accessToken],
    queryFn: () => fetchAdminHazardReportDetail(selectedReportId as number, accessToken),
    enabled: hasToken && selectedReportId != null,
    retry: false,
  });

  const approveMutation = useMutation({
    mutationFn: (reportId: number) => approveAdminHazardReport(reportId, accessToken),
    onSuccess: (response) => {
      setSelectedReportId(response.reportId);
      void queryClient.invalidateQueries({ queryKey: ["admin-hazard-reports"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-hazard-report-detail", response.reportId] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (reportId: number) => rejectAdminHazardReport(reportId, accessToken),
    onSuccess: (response) => {
      setSelectedReportId(response.reportId);
      void queryClient.invalidateQueries({ queryKey: ["admin-hazard-reports"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-hazard-report-detail", response.reportId] });
    },
  });

  function changeStatus(nextStatus: "" | HazardReportStatus) {
    setStatus(nextStatus);
    setCursorStack([null]);
    setSelectedReportId(null);
  }

  const detail = detailQuery.data;
  const actionDisabled =
    !detail ||
    detail.status !== "PENDING" ||
    approveMutation.isPending ||
    rejectMutation.isPending;

  return (
    <div className="hazard-page">
      <section className="admin-token-panel">
        <div className="admin-session-panel">
          <div>
            <strong>Admin</strong>
            <span>{adminPrincipal.userId} · {adminPrincipal.role}</span>
          </div>
          <button type="button" onClick={onLogout}>로그아웃</button>
        </div>
      </section>

      <div className="hazard-layout">
        <section className="hazard-list-panel">
          <div className="panel-toolbar">
            <label>
              상태
              <select value={status} onChange={(event) => changeStatus(event.target.value as "" | HazardReportStatus)}>
                {reportStatusOptions.map((option) => (
                  <option key={option || "ALL"} value={option}>
                    {option ? reportStatusLabel[option] : "전체"}
                  </option>
                ))}
              </select>
            </label>
            <button type="button" onClick={() => void reportsQuery.refetch()} disabled={!hasToken || reportsQuery.isFetching}>
              새로고침
            </button>
          </div>

          {reportsQuery.error instanceof Error && <p className="error-box">{reportsQuery.error.message}</p>}
          {reportsQuery.isLoading && <p className="muted">제보 목록을 불러오는 중입니다.</p>}

          <div className="hazard-table" role="table" aria-label="도로 상태 제보 목록">
            <div className="hazard-table-head" role="row">
              <span>상태</span>
              <span>유형</span>
              <span>제보자</span>
              <span>등록일</span>
            </div>
            {(reportsQuery.data?.content ?? []).map((report) => (
              <HazardReportRow
                key={report.reportId}
                report={report}
                selected={report.reportId === selectedReportId}
                onClick={() => setSelectedReportId(report.reportId)}
              />
            ))}
          </div>

          {!reportsQuery.isLoading && hasToken && !reportsQuery.data?.content.length && (
            <p className="muted">조회된 제보가 없습니다.</p>
          )}

          <div className="pagination-row">
            <button
              type="button"
              disabled={cursorStack.length <= 1}
              onClick={() => {
                setCursorStack((value) => value.slice(0, -1));
                setSelectedReportId(null);
              }}
            >
              이전
            </button>
            <button
              type="button"
              disabled={!reportsQuery.data?.hasNext || !reportsQuery.data.nextCursor}
              onClick={() => {
                if (!reportsQuery.data?.nextCursor) return;
                setCursorStack((value) => [...value, reportsQuery.data.nextCursor]);
                setSelectedReportId(null);
              }}
            >
              다음
            </button>
          </div>
        </section>

        <aside className="hazard-detail-panel">
          <h3>제보 상세</h3>
          {!selectedReportId && <p className="muted">목록에서 제보를 선택하세요.</p>}
          {detailQuery.isLoading && <p className="muted">상세를 불러오는 중입니다.</p>}
          {detailQuery.error instanceof Error && <p className="error-box">{detailQuery.error.message}</p>}
          {selectedReport && !detail && !detailQuery.isLoading && <SummaryDetails report={selectedReport} />}
          {detail && (
            <>
              <SummaryDetails report={detail} />
              <p className="hazard-description">{detail.description}</p>
              <div className="hazard-images">
                {detail.imageUrls.map((imageUrl) => (
                  <img key={imageUrl} src={imageUrl} alt={`제보 ${detail.reportId} 첨부 이미지`} />
                ))}
                {!detail.imageUrls.length && <span className="muted">첨부 이미지 없음</span>}
              </div>
              <div className="button-row">
                <button
                  className="primary"
                  type="button"
                  disabled={actionDisabled}
                  onClick={() => approveMutation.mutate(detail.reportId)}
                >
                  승인
                </button>
                <button
                  className="danger"
                  type="button"
                  disabled={actionDisabled}
                  onClick={() => rejectMutation.mutate(detail.reportId)}
                >
                  반려
                </button>
              </div>
              {(approveMutation.error instanceof Error || rejectMutation.error instanceof Error) && (
                <p className="error-box">
                  {approveMutation.error instanceof Error ? approveMutation.error.message : rejectMutation.error?.message}
                </p>
              )}
            </>
          )}
        </aside>
      </div>
    </div>
  );
}

function HazardReportRow({
  report,
  selected,
  onClick,
}: {
  report: AdminHazardReportSummary;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <button className={`hazard-row ${selected ? "selected" : ""}`} type="button" role="row" onClick={onClick}>
      <span className={`status-pill ${report.status.toLowerCase()}`}>{reportStatusLabel[report.status]}</span>
      <span>{reportTypeLabel[report.reportType] ?? report.reportType}</span>
      <span title={report.reporterUserId}>{shortId(report.reporterUserId)}</span>
      <span>{formatDateTime(report.createdAt)}</span>
    </button>
  );
}

function SummaryDetails({ report }: { report: AdminHazardReportSummary | AdminHazardReportDetail }) {
  return (
    <dl className="attribute-detail-list">
      <AttributeRow label="report" value={String(report.reportId)} />
      <AttributeRow label="상태" value={reportStatusLabel[report.status]} />
      <AttributeRow label="유형" value={reportTypeLabel[report.reportType] ?? report.reportType} />
      <AttributeRow label="제보자" value={report.reporterUserId} />
      <AttributeRow label="좌표" value={`${report.reportPoint.lat.toFixed(6)}, ${report.reportPoint.lng.toFixed(6)}`} />
      <AttributeRow label="등록일" value={formatDateTime(report.createdAt)} />
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

function shortId(value: string) {
  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
