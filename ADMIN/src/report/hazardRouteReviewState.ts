import type {
  AdminHazardRouteReview,
  AdminHazardRouteReviewIntent,
  AdminRoadSegmentAttributesUpdateRequest,
  AdminRoutingApplyStatus,
  HazardReportStatus,
  UpdateAdminHazardRouteReviewRequest,
} from "../types";

const hazardRouteReviewStoragePrefix = "busan-eumgil-ADMIN:hazard-route-review:";

export type HazardRouteReviewIntent = "approve" | "restore";
export type HazardRouteReviewStage = "IN_PROGRESS" | "COMPLETED";
export type HazardRouteReviewTone = "blue" | "orange" | "green" | "red" | "purple" | "gray";
export type HazardDisplayStatusKey = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "REJECTED" | "RESTORED";

export interface HazardRouteReviewRecord {
  reportId: number;
  intent: HazardRouteReviewIntent;
  stage: HazardRouteReviewStage;
  reviewerUserId: string;
  startedAt: string;
  updatedAt: string;
  completedAt: string | null;
  selectedSegmentEdgeId: string | null;
  segmentDrafts: Record<string, AdminRoadSegmentAttributesUpdateRequest>;
  routingApplyStatus?: AdminRoutingApplyStatus | null;
  routingApplyMessage?: string | null;
}

export interface HazardDisplayStatus {
  key: HazardDisplayStatusKey;
  label: string;
  tone: HazardRouteReviewTone;
}

export function hazardRouteReviewStorageKey(reportId: number) {
  return `${hazardRouteReviewStoragePrefix}${reportId}`;
}

export function loadStoredHazardRouteReview(reportId: number): HazardRouteReviewRecord | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(hazardRouteReviewStorageKey(reportId));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<HazardRouteReviewRecord> | null;
    if (!parsed || typeof parsed !== "object") {
      return null;
    }
    if (typeof parsed.reportId !== "number" || (parsed.intent !== "approve" && parsed.intent !== "restore")) {
      return null;
    }
    return {
      reportId: parsed.reportId,
      intent: parsed.intent,
      stage: parsed.stage === "COMPLETED" ? "COMPLETED" : "IN_PROGRESS",
      reviewerUserId: typeof parsed.reviewerUserId === "string" ? parsed.reviewerUserId : "ADMIN",
      startedAt: typeof parsed.startedAt === "string" ? parsed.startedAt : new Date().toISOString(),
      updatedAt: typeof parsed.updatedAt === "string" ? parsed.updatedAt : typeof parsed.startedAt === "string" ? parsed.startedAt : new Date().toISOString(),
      completedAt: typeof parsed.completedAt === "string" ? parsed.completedAt : null,
      selectedSegmentEdgeId: typeof parsed.selectedSegmentEdgeId === "string" ? parsed.selectedSegmentEdgeId : null,
      segmentDrafts: parsed.segmentDrafts && typeof parsed.segmentDrafts === "object" ? parsed.segmentDrafts : {},
      routingApplyStatus: isAdminRoutingApplyStatus(parsed.routingApplyStatus) ? parsed.routingApplyStatus : null,
      routingApplyMessage: typeof parsed.routingApplyMessage === "string" ? parsed.routingApplyMessage : null,
    };
  } catch {
    return null;
  }
}

export function storeHazardRouteReview(review: HazardRouteReviewRecord | null) {
  if (typeof window === "undefined" || !review) return;
  window.localStorage.setItem(hazardRouteReviewStorageKey(review.reportId), JSON.stringify(review));
}

export function clearStoredHazardRouteReview(reportId: number) {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(hazardRouteReviewStorageKey(reportId));
}

export function startHazardRouteReview({
  reportId,
  intent,
  reviewerUserId,
  now,
  existing,
}: {
  reportId: number;
  intent: HazardRouteReviewIntent;
  reviewerUserId: string;
  now: string;
  existing?: HazardRouteReviewRecord | null;
}): HazardRouteReviewRecord {
  const canResumeExisting = existing?.intent === intent && existing.stage === "IN_PROGRESS";
  return {
    reportId,
    intent,
    stage: "IN_PROGRESS",
    reviewerUserId,
    startedAt: canResumeExisting ? existing.startedAt : now,
    updatedAt: now,
    completedAt: null,
    selectedSegmentEdgeId: existing?.selectedSegmentEdgeId ?? null,
    segmentDrafts: existing?.segmentDrafts ?? {},
    routingApplyStatus: existing?.routingApplyStatus ?? null,
    routingApplyMessage: existing?.routingApplyMessage ?? null,
  };
}

export function hydrateHazardRouteReviewRecord(review?: AdminHazardRouteReview | null): HazardRouteReviewRecord | null {
  if (!review) {
    return null;
  }

  return {
    reportId: review.reportId,
    intent: fromAdminHazardRouteReviewIntent(review.intent),
    stage: review.stage,
    reviewerUserId: review.reviewerUserId,
    startedAt: review.startedAt,
    updatedAt: review.updatedAt,
    completedAt: review.completedAt,
    routingApplyStatus: review.routingApplyStatus ?? null,
    routingApplyMessage: review.routingApplyMessage ?? null,
    selectedSegmentEdgeId: review.selectedSegmentEdgeId == null ? null : String(review.selectedSegmentEdgeId),
    segmentDrafts: review.segmentDrafts.reduce<Record<string, AdminRoadSegmentAttributesUpdateRequest>>((drafts, segmentDraft) => {
      drafts[String(segmentDraft.edgeId)] = {
        walkAccess: segmentDraft.walkAccess ?? null,
        brailleBlockState: segmentDraft.brailleBlockState ?? null,
        audioSignalState: segmentDraft.audioSignalState ?? null,
        widthState: segmentDraft.widthState ?? null,
        surfaceState: segmentDraft.surfaceState ?? null,
        stairsState: segmentDraft.stairsState ?? null,
        signalState: segmentDraft.signalState ?? null,
      };
      return drafts;
    }, {}),
  };
}

export function toAdminHazardRouteReviewIntent(intent: HazardRouteReviewIntent): AdminHazardRouteReviewIntent {
  return intent === "restore" ? "RESTORE" : "APPROVE";
}

export function toAdminHazardRouteReviewUpdateRequest(review: HazardRouteReviewRecord): UpdateAdminHazardRouteReviewRequest {
  return {
    selectedSegmentEdgeId: review.selectedSegmentEdgeId == null ? null : Number(review.selectedSegmentEdgeId),
    segmentDrafts: Object.entries(review.segmentDrafts).map(([edgeId, draft]) => ({
      edgeId: Number(edgeId),
      walkAccess: draft.walkAccess ?? null,
      brailleBlockState: draft.brailleBlockState ?? null,
      audioSignalState: draft.audioSignalState ?? null,
      widthState: draft.widthState ?? null,
      surfaceState: draft.surfaceState ?? null,
      stairsState: draft.stairsState ?? null,
      signalState: draft.signalState ?? null,
    })),
  };
}

export function selectHazardRouteReviewSegment(
  review: HazardRouteReviewRecord,
  edgeId: string | number,
  now: string,
) {
  return {
    ...review,
    selectedSegmentEdgeId: String(edgeId),
    updatedAt: now,
  };
}

export function updateHazardRouteReviewSegmentDraft(
  review: HazardRouteReviewRecord,
  edgeId: string | number,
  draft: AdminRoadSegmentAttributesUpdateRequest,
  now: string,
): HazardRouteReviewRecord {
  const normalizedEdgeId = String(edgeId);
  return {
    ...review,
    selectedSegmentEdgeId: normalizedEdgeId,
    updatedAt: now,
    segmentDrafts: {
      ...review.segmentDrafts,
      [normalizedEdgeId]: draft,
    },
  };
}

export function completeHazardRouteReview(review: HazardRouteReviewRecord, now: string): HazardRouteReviewRecord {
  return {
    ...review,
    stage: "COMPLETED",
    updatedAt: now,
    completedAt: now,
  };
}

export function routeReviewCompletionMessage(status?: AdminRoutingApplyStatus | null) {
  switch (status) {
    case "APPLIED":
      return "검수 완료 및 경로 반영이 완료되었습니다.";
    case "APPLIED_WITH_WARNING":
      return "검수는 완료됐고 일부 경로 반영 경고가 있습니다. 운영 상태를 확인해 주세요.";
    case "FAILED":
      return "검수는 완료됐지만 경로 즉시 반영에 실패했습니다. 운영 상태를 확인해 주세요.";
    case "SKIPPED":
      return "검수는 완료됐고 즉시 반영 대상 변경은 없습니다.";
    default:
      return "검수가 완료되었습니다.";
  }
}

export function routeReviewCompletionClassName(status?: AdminRoutingApplyStatus | null) {
  switch (status) {
    case "FAILED":
      return "error-box";
    case "APPLIED_WITH_WARNING":
      return "warning-box";
    case "APPLIED":
      return "success-box";
    case "SKIPPED":
      return "info-box";
    default:
      return "info-box";
  }
}

export function deriveHazardDisplayStatus(
  baseStatus: HazardReportStatus,
  review?: HazardRouteReviewRecord | null,
): HazardDisplayStatus {
  if (review?.stage === "IN_PROGRESS") {
    return { key: "IN_PROGRESS", label: "진행중", tone: "blue" };
  }
  if (review?.stage === "COMPLETED" && review.intent === "restore") {
    return { key: "RESTORED", label: "원복 완료", tone: "purple" };
  }
  if (review?.stage === "COMPLETED" && review.intent === "approve") {
    return { key: "COMPLETED", label: "완료", tone: "green" };
  }
  if (baseStatus === "APPROVED") {
    return { key: "COMPLETED", label: "완료", tone: "green" };
  }
  if (baseStatus === "REJECTED") {
    return { key: "REJECTED", label: "반려", tone: "red" };
  }
  return { key: "PENDING", label: "대기", tone: "orange" };
}

export function canStartHazardApprove(
  baseStatus: HazardReportStatus,
  review?: HazardRouteReviewRecord | null,
) {
  if (review?.stage === "IN_PROGRESS") {
    return false;
  }
  if (review?.stage === "COMPLETED" && review.intent === "approve") {
    return false;
  }
  return baseStatus === "PENDING" || baseStatus === "REJECTED";
}

export function canStartHazardRestore(
  baseStatus: HazardReportStatus,
  review?: HazardRouteReviewRecord | null,
) {
  if (review?.stage === "IN_PROGRESS") {
    return false;
  }
  if (review?.stage === "COMPLETED" && review.intent === "restore") {
    return false;
  }
  if (review?.stage === "COMPLETED" && review.intent === "approve") {
    return true;
  }
  return baseStatus === "APPROVED";
}

export function isHazardRestorePending(review?: HazardRouteReviewRecord | null) {
  return review?.intent === "restore" && (review.stage === "IN_PROGRESS" || review.stage === "COMPLETED");
}

export function isHazardReviewActive(review?: HazardRouteReviewRecord | null) {
  return review?.stage === "IN_PROGRESS";
}

export function hazardRouteReviewIntentLabel(intent: HazardRouteReviewIntent) {
  return intent === "restore" ? "원상복구 검수" : "승인 검수";
}

function fromAdminHazardRouteReviewIntent(intent: AdminHazardRouteReviewIntent): HazardRouteReviewIntent {
  return intent === "RESTORE" ? "restore" : "approve";
}

function isAdminRoutingApplyStatus(value: unknown): value is AdminRoutingApplyStatus {
  return value === "SKIPPED"
    || value === "APPLIED"
    || value === "APPLIED_WITH_WARNING"
    || value === "FAILED";
}
