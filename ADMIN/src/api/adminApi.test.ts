import { afterEach, describe, expect, it, vi } from "vitest";
import {
  completeAdminHazardRouteReview,
  fetchAdminHazardReportDetail,
  startAdminHazardRouteReview,
  updateAdminHazardRouteReview,
} from "./adminApi";

describe("admin hazard route review API", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches latest route review with hazard report detail", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        status: "OK",
        message: "ok",
        data: {
          reportId: 1,
          reporterUserId: "user-1",
          reportType: "SIDEWALK_MISSING",
          description: null,
          reportPoint: { lat: 35.1, lng: 129.1 },
          status: "PENDING",
          createdAt: "2026-05-18T10:00:00",
          imageUrls: [],
          latestRouteReview: {
            reviewId: 8,
            reportId: 1,
            intent: "APPROVE",
            stage: "IN_PROGRESS",
            reportStatus: "PENDING",
            reviewerUserId: "admin-1",
            gu: "부산진구",
            dong: "부전동",
            selectedSegmentEdgeId: 41231,
            startedAt: "2026-05-18T10:05:00",
            updatedAt: "2026-05-18T10:06:00",
            completedAt: null,
            segmentDrafts: [],
          },
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const response = await fetchAdminHazardReportDetail(1, "token");

    expect(response.latestRouteReview?.reviewId).toBe(8);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/admin/hazard-reports/1"),
      expect.objectContaining({
        credentials: "include",
        headers: expect.objectContaining({
          Authorization: "Bearer token",
        }),
      }),
    );
  });

  it("starts, updates, and completes route review through dedicated endpoints", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          status: "OK",
          message: "ok",
          data: {
            reviewId: 8,
            reportId: 1,
            intent: "APPROVE",
            stage: "IN_PROGRESS",
            reportStatus: "PENDING",
            reviewerUserId: "admin-1",
            gu: "부산진구",
            dong: "부전동",
            selectedSegmentEdgeId: null,
            startedAt: "2026-05-18T10:05:00",
            updatedAt: "2026-05-18T10:05:00",
            completedAt: null,
            segmentDrafts: [],
          },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          status: "OK",
          message: "ok",
          data: {
            reviewId: 8,
            reportId: 1,
            intent: "APPROVE",
            stage: "IN_PROGRESS",
            reportStatus: "PENDING",
            reviewerUserId: "admin-1",
            gu: "부산진구",
            dong: "부전동",
            selectedSegmentEdgeId: 41231,
            startedAt: "2026-05-18T10:05:00",
            updatedAt: "2026-05-18T10:06:00",
            completedAt: null,
            segmentDrafts: [],
          },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          status: "OK",
          message: "ok",
          data: {
            reviewId: 8,
            reportId: 1,
            intent: "APPROVE",
            stage: "COMPLETED",
            reportStatus: "APPROVED",
            reviewerUserId: "admin-1",
            gu: "부산진구",
            dong: "부전동",
            selectedSegmentEdgeId: 41231,
            startedAt: "2026-05-18T10:05:00",
            updatedAt: "2026-05-18T10:07:00",
            completedAt: "2026-05-18T10:07:00",
            segmentDrafts: [],
          },
        }),
      });
    vi.stubGlobal("fetch", fetchMock);

    await startAdminHazardRouteReview(
      1,
      { intent: "APPROVE", gu: "부산진구", dong: "부전동" },
      "token",
    );
    await updateAdminHazardRouteReview(
      1,
      { selectedSegmentEdgeId: 41231, segmentDrafts: [] },
      "token",
    );
    const completed = await completeAdminHazardRouteReview(1, "token");

    expect(completed.stage).toBe("COMPLETED");
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining("/admin/hazard-reports/1/route-review/start"),
      expect.objectContaining({ method: "POST" }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining("/admin/hazard-reports/1/route-review"),
      expect.objectContaining({ method: "PATCH" }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      expect.stringContaining("/admin/hazard-reports/1/route-review/complete"),
      expect.objectContaining({ method: "POST" }),
    );
  });
});
