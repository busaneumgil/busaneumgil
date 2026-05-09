import type {
  AreaOption,
  AdminHazardReportDetail,
  AdminHazardReportListResponse,
  AdminHazardReportStatusResponse,
  AdminMeResponse,
  FacilityPayload,
  HazardReportStatus,
  SegmentPayload,
} from "../types";

const configuredBackendApiUrl = import.meta.env.VITE_BACKEND_API_URL as string | undefined;

function defaultBackendApiUrl() {
  if (typeof window === "undefined") {
    return "http://localhost:8080";
  }
  if (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1") {
    return "http://localhost:8080";
  }
  return "https://api.prod.busaneumgil.com";
}

export const backendApiUrl = (configuredBackendApiUrl || defaultBackendApiUrl()).replace(/\/$/, "");

export const adminAccessTokenStorageKey = "busan-eumgil-ADMIN:access-token";

interface ApiResponse<T> {
  status: string;
  data: T;
  message: string;
}

interface FetchAdminHazardReportsParams {
  status?: HazardReportStatus | "";
  cursor?: number | null;
  size?: number;
  accessToken: string;
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${backendApiUrl}${path}`, init);
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null;
  if (!response.ok) {
    throw new Error(body?.message || `${response.status} ${response.statusText}`);
  }
  if (!body) {
    throw new Error("응답을 읽을 수 없습니다.");
  }
  return body.data;
}

async function requestAdminJson<T>(path: string, accessToken: string, init?: RequestInit): Promise<T> {
  const normalizedToken = normalizeAdminAccessToken(accessToken);
  if (!normalizedToken) {
    throw new Error("Access Token을 입력하세요.");
  }
  return requestJson<T>(path, {
    ...init,
    headers: {
      ...init?.headers,
      Authorization: `Bearer ${normalizedToken}`,
    },
  });
}

export function getStoredAdminAccessToken() {
  if (typeof window === "undefined") return "";
  return normalizeAdminAccessToken(window.localStorage.getItem(adminAccessTokenStorageKey) || "");
}

export function normalizeAdminAccessToken(accessToken: string) {
  return accessToken.trim().replace(/^Bearer\s+/i, "");
}

export function storeAdminAccessToken(accessToken: string) {
  if (typeof window === "undefined") return;
  const normalizedToken = normalizeAdminAccessToken(accessToken);
  if (!normalizedToken) {
    window.localStorage.removeItem(adminAccessTokenStorageKey);
    return;
  }
  window.localStorage.setItem(adminAccessTokenStorageKey, normalizedToken);
}

export async function fetchAdminMe(accessToken: string): Promise<AdminMeResponse> {
  return requestAdminJson<AdminMeResponse>("/admin/me", accessToken);
}

export async function fetchAdminAreas(accessToken: string): Promise<AreaOption[]> {
  const response = await requestAdminJson<{ areas: AreaOption[] }>("/admin/areas", accessToken);
  return response.areas;
}

export async function fetchAdminRoadNetworkPayload({
  gu,
  dong,
  accessToken,
  limit = 10000,
}: {
  gu?: string;
  dong?: string;
  accessToken: string;
  limit?: number;
}): Promise<SegmentPayload> {
  const params = new URLSearchParams({ limit: String(limit) });
  if (gu && dong) {
    params.set("gu", gu);
    params.set("dong", dong);
  }
  return requestAdminJson<SegmentPayload>(`/admin/road-network/segments?${params.toString()}`, accessToken);
}

export async function fetchAdminFacilityPayload({
  accessToken,
  limit = 20000,
}: {
  accessToken: string;
  limit?: number;
}): Promise<FacilityPayload> {
  const params = new URLSearchParams({ limit: String(limit) });
  return requestAdminJson<FacilityPayload>(`/admin/places/facilities?${params.toString()}`, accessToken);
}

export async function fetchAdminHazardReports({
  status,
  cursor,
  size = 10,
  accessToken,
}: FetchAdminHazardReportsParams): Promise<AdminHazardReportListResponse> {
  const params = new URLSearchParams({ size: String(size) });
  if (status) params.set("status", status);
  if (cursor) params.set("cursor", String(cursor));
  return requestAdminJson<AdminHazardReportListResponse>(`/admin/hazard-reports?${params.toString()}`, accessToken);
}

export async function fetchAdminHazardReportDetail(
  reportId: number,
  accessToken: string,
): Promise<AdminHazardReportDetail> {
  return requestAdminJson<AdminHazardReportDetail>(`/admin/hazard-reports/${reportId}`, accessToken);
}

export async function approveAdminHazardReport(
  reportId: number,
  accessToken: string,
): Promise<AdminHazardReportStatusResponse> {
  return requestAdminJson<AdminHazardReportStatusResponse>(`/admin/hazard-reports/${reportId}/approve`, accessToken, {
    method: "PATCH",
  });
}

export async function rejectAdminHazardReport(
  reportId: number,
  accessToken: string,
): Promise<AdminHazardReportStatusResponse> {
  return requestAdminJson<AdminHazardReportStatusResponse>(`/admin/hazard-reports/${reportId}/reject`, accessToken, {
    method: "PATCH",
  });
}
