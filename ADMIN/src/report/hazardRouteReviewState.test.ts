import { afterEach, describe, expect, it, vi } from "vitest";
import {
  canStartHazardApprove,
  canStartHazardRestore,
  completeHazardRouteReview,
  deriveHazardDisplayStatus,
  hazardRouteReviewIntentLabel,
  hydrateHazardRouteReviewRecord,
  loadStoredHazardRouteReview,
  routeReviewCompletionClassName,
  routeReviewCompletionMessage,
  startHazardRouteReview,
  storeHazardRouteReview,
  updateHazardRouteReviewSegmentDraft,
} from "./hazardRouteReviewState";

describe("hazard route review workflow state", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("stores and restores in-progress drafts for resume", () => {
    const storage = new Map<string, string>();
    vi.stubGlobal("window", {
      localStorage: {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => storage.set(key, value),
        removeItem: (key: string) => storage.delete(key),
      },
    });

    const review = updateHazardRouteReviewSegmentDraft(startHazardRouteReview({
      reportId: 12,
      intent: "approve",
      reviewerUserId: "admin-1",
      now: "2026-05-18T02:00:00.000Z",
    }), "edge-7", { walkAccess: "NO", stairsState: "YES" }, "2026-05-18T02:01:00.000Z");

    storeHazardRouteReview(review);

    expect(loadStoredHazardRouteReview(12)).toMatchObject({
      reportId: 12,
      intent: "approve",
      stage: "IN_PROGRESS",
      selectedSegmentEdgeId: "edge-7",
      segmentDrafts: {
        "edge-7": {
          walkAccess: "NO",
          stairsState: "YES",
        },
      },
    });
  });

  it("derives list/detail status from local review progress before backend wiring is finished", () => {
    const review = startHazardRouteReview({
      reportId: 7,
      intent: "approve",
      reviewerUserId: "admin-2",
      now: "2026-05-18T03:00:00.000Z",
    });

    expect(deriveHazardDisplayStatus("PENDING", review)).toEqual({
      key: "IN_PROGRESS",
      label: "진행중",
      tone: "blue",
    });

    expect(deriveHazardDisplayStatus("PENDING", completeHazardRouteReview(review, "2026-05-18T03:10:00.000Z"))).toEqual({
      key: "COMPLETED",
      label: "완료",
      tone: "green",
    });

    expect(deriveHazardDisplayStatus("APPROVED", completeHazardRouteReview({
      ...review,
      intent: "restore",
    }, "2026-05-18T03:12:00.000Z"))).toEqual({
      key: "RESTORED",
      label: "원복 완료",
      tone: "purple",
    });
  });

  it("only allows restore review for already approved reports", () => {
    expect(canStartHazardRestore("PENDING")).toBe(false);
    expect(canStartHazardRestore("APPROVED")).toBe(true);
    expect(canStartHazardRestore("REJECTED")).toBe(false);
    expect(canStartHazardRestore("PENDING", completeHazardRouteReview(startHazardRouteReview({
      reportId: 3,
      intent: "approve",
      reviewerUserId: "admin-3",
      now: "2026-05-18T03:20:00.000Z",
    }), "2026-05-18T03:35:00.000Z"))).toBe(true);
    expect(hazardRouteReviewIntentLabel("restore")).toBe("원상복구 검수");
  });

  it("allows approve review for pending and rejected reports", () => {
    const review = startHazardRouteReview({
      reportId: 4,
      intent: "approve",
      reviewerUserId: "admin-4",
      now: "2026-05-18T03:40:00.000Z",
    });

    expect(canStartHazardApprove("PENDING")).toBe(true);
    expect(canStartHazardApprove("REJECTED")).toBe(true);
    expect(canStartHazardApprove("APPROVED")).toBe(false);
    expect(canStartHazardApprove("REJECTED", review)).toBe(false);
    expect(canStartHazardApprove("REJECTED", completeHazardRouteReview(review, "2026-05-18T03:55:00.000Z"))).toBe(false);
  });

  it("hydrates latest server route review into local workflow state", () => {
    const review = hydrateHazardRouteReviewRecord({
      reviewId: 21,
      reportId: 12,
      intent: "RESTORE",
      stage: "IN_PROGRESS",
      reportStatus: "APPROVED",
      reviewerUserId: "admin-8",
      gu: "부산진구",
      dong: "부전동",
      selectedSegmentEdgeId: 41231,
      startedAt: "2026-05-18T10:00:00",
      updatedAt: "2026-05-18T10:05:00",
      completedAt: null,
      routingApplyStatus: "FAILED",
      routingApplyMessage: "reload failed",
      segmentDrafts: [
        {
          edgeId: 41231,
          walkAccess: "NO",
          brailleBlockState: "UNKNOWN",
          audioSignalState: "UNKNOWN",
          widthState: "NARROW",
          surfaceState: null,
          stairsState: "YES",
          signalState: "UNKNOWN",
        },
      ],
    });

    expect(review).toEqual({
      reportId: 12,
      intent: "restore",
      stage: "IN_PROGRESS",
      reviewerUserId: "admin-8",
      startedAt: "2026-05-18T10:00:00",
      updatedAt: "2026-05-18T10:05:00",
      completedAt: null,
      routingApplyStatus: "FAILED",
      routingApplyMessage: "reload failed",
      selectedSegmentEdgeId: "41231",
      segmentDrafts: {
        "41231": {
          walkAccess: "NO",
          brailleBlockState: "UNKNOWN",
          audioSignalState: "UNKNOWN",
          widthState: "NARROW",
          surfaceState: null,
          stairsState: "YES",
          signalState: "UNKNOWN",
        },
      },
    });
  });

  it("formats route review completion messages from routing apply status", () => {
    expect(routeReviewCompletionMessage("APPLIED")).toBe("검수 완료 및 경로 반영이 완료되었습니다.");
    expect(routeReviewCompletionMessage("APPLIED_WITH_WARNING")).toContain("경고");
    expect(routeReviewCompletionMessage("FAILED")).toContain("실패");
    expect(routeReviewCompletionMessage("SKIPPED")).toContain("즉시 반영 대상 변경은 없습니다");
  });

  it("maps route review completion status to visual severity classes", () => {
    expect(routeReviewCompletionClassName("FAILED")).toBe("error-box");
    expect(routeReviewCompletionClassName("APPLIED_WITH_WARNING")).toBe("warning-box");
    expect(routeReviewCompletionClassName("APPLIED")).toBe("success-box");
    expect(routeReviewCompletionClassName("SKIPPED")).toBe("info-box");
  });
});
