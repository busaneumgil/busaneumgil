import type { AdminRoadSegmentAttributesUpdateRequest, HazardReportStatus } from "../types";

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

export function isHazardReviewActive(review?: HazardRouteReviewRecord | null) {
  return review?.stage === "IN_PROGRESS";
}

export function hazardRouteReviewIntentLabel(intent: HazardRouteReviewIntent) {
  return intent === "restore" ? "원상복구 검수" : "승인 검수";
}
