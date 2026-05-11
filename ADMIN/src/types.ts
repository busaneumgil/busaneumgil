export type AdminRole = "ADMIN";

export type SocialProvider = "KAKAO" | "NAVER" | "GOOGLE";

export interface AuthTestConfig {
  kakaoJavaScriptKey: string;
  naverClientId: string;
  googleClientId: string;
}

export interface SocialLoginResponse {
  signupRequired: boolean;
  signupToken: string | null;
  accessToken: string | null;
  refreshToken: string | null;
  userId: string | null;
  selectedPrimaryUserType: string | null;
  selectedMobilitySubtype: string | null;
}

export type WorkStatus = "TODO" | "DRAFT" | "REVIEW" | "APPLIED" | "REOPENED";

export type AdminPage = "network" | "facilities" | "hazards";

export type EditableSegmentType = "SIDE_LINE" | "CROSS_WALK";

export interface Assignment {
  assignmentId: string;
  gu: string;
  dong: string;
  assigneeId: string;
  assigneeName: string;
  status: WorkStatus;
  changeSummary: ChangeSummary;
  assigneeMemo: string;
  adminMemo: string;
  updatedAt: string;
  reviewedAt?: string;
  appliedAt?: string;
}

export interface ChangeSummary {
  addedSideLine: number;
  addedCrossWalk: number;
  deletedSegments: number;
  deletedNodes: number;
}

export interface AreaOption {
  gu: string;
  dong: string;
}

export interface SegmentFeature {
  type: "Feature";
  bbox?: [number, number, number, number];
  geometry: {
    type: "LineString";
    coordinates: Array<[number, number]>;
  };
  properties: {
    edgeId: number | string;
    fromNodeId?: number | string;
    toNodeId?: number | string;
    segmentType?: EditableSegmentType | "SIDE_WALK" | "TRANSITION_CONNECTOR" | string;
    lengthMeter?: number | string;
  };
}

export interface SegmentPayload {
  summary?: {
    segmentCount?: number;
    nodeCount?: number;
    visibleSegmentCount?: number;
    bridgeCandidateCount?: number | null;
    visibleBridgeCandidateCount?: number;
  };
  csv?: {
    segmentCsv?: string;
    nodeCsv?: string;
  };
  bbox?: [number, number, number, number] | null;
  segments: {
    type: "FeatureCollection";
    features: SegmentFeature[];
  };
  bridges?: {
    type: "FeatureCollection";
    features: BridgeFeature[];
  };
}

export interface BridgeFeature {
  type: "Feature";
  bbox?: [number, number, number, number];
  geometry: {
    type: "LineString";
    coordinates: Array<[number, number]>;
  };
  properties: {
    candidateId: string;
    type: "PROPOSED_BRIDGE";
    priority?: "AUTO" | "REVIEW" | string;
    fromNodeId?: string;
    toEdgeId?: string;
    distanceMeter?: number;
    markerPoint?: [number, number];
    reason?: string;
  };
}

export interface BridgePayload {
  summary?: {
    bridgeCandidateCount?: number | null;
    visibleBridgeCandidateCount?: number;
    bridgeMaxDistanceMeter?: number;
  };
  bridges: {
    type: "FeatureCollection";
    features: BridgeFeature[];
  };
}

export interface RoadAttributeFeature {
  type: "Feature";
  bbox?: [number, number, number, number];
  geometry: {
    type: "LineString";
    coordinates: Array<[number, number]>;
  };
  properties: {
    handoffEdgeId: string;
    sourceId: string;
    districtGu: string;
    name?: string;
    roadName?: string;
    roadDivisionLabel?: string;
    pavementQualityLabel?: string;
    surfaceType?: string;
    laneCount?: number | null;
    widthMeter?: number | null;
    widthLevel?: string;
    widthLevelLabel?: string;
    slopeMean?: number | null;
    slopeMax?: number | null;
    slopeLevel?: string;
    slopeRange?: string;
    slopeLevelLabel?: string;
    riskLevel?: string;
    ufid?: string;
  };
}

export interface RoadAttributePayload {
  summary?: {
    roadAttributeCount?: number;
    guRoadAttributeCount?: number;
    visibleRoadAttributeCount?: number;
  };
  csv?: {
    roadAttributeCsv?: string;
  };
  bbox?: [number, number, number, number] | null;
  roadAttributes: {
    type: "FeatureCollection";
    features: RoadAttributeFeature[];
  };
}

export interface FacilityFeature {
  type: "Feature";
  bbox?: [number, number, number, number];
  geometry: {
    type: "Point";
    coordinates: [number, number];
  };
  properties: {
    placeId: string;
    name: string;
    category: string;
    address: string;
    providerPlaceId?: string;
  };
}

export interface FacilityPayload {
  summary?: {
    facilityCount?: number;
    visibleFacilityCount?: number;
    providerPlaceIdCount?: number;
    categoryCounts?: Record<string, number>;
    visibleCategoryCounts?: Record<string, number>;
  };
  csv?: {
    facilityCsv?: string;
  };
  bbox?: [number, number, number, number] | null;
  facilities: {
    type: "FeatureCollection";
    features: FacilityFeature[];
  };
}

export type ReferenceLayerKey = "roadAttributes" | "stairs" | "audioSignals" | "brailleBlocks";

export interface ReferencePointFeature {
  type: "Feature";
  bbox?: [number, number, number, number];
  geometry: {
    type: "Point";
    coordinates: [number, number];
  };
  properties: {
    sourceId: string;
    layerType: Exclude<ReferenceLayerKey, "roadAttributes">;
    label: string;
    state?: string;
    distanceMeter?: number | null;
    matchConfidence?: string;
  };
}

export interface ReferencePointPayload {
  summary?: {
    referencePointCount?: number;
    visibleReferencePointCount?: number;
    stateCounts?: Record<string, number>;
    visibleStateCounts?: Record<string, number>;
  };
  bbox?: [number, number, number, number] | null;
  points: {
    type: "FeatureCollection";
    features: ReferencePointFeature[];
  };
}

export type EditAction =
  | {
      action: "add_segment";
      segmentType: EditableSegmentType;
      geom: { type: "LineString"; coordinates: Array<[number, number]> };
    }
  | { action: "delete_segment"; edgeId: string | number; reason?: string }
  | { action: "delete_node"; vertexId: string | number; reason?: string };

export interface ManualEditDocument {
  version: "ADMIN-draft-v1";
  assignmentId: string;
  gu: string;
  dong: string;
  role: AdminRole;
  createdAt: string;
  edits: EditAction[];
}

export interface RoadNetworkEditApplyResponse {
  addedSegments: number;
  deletedSegments: number;
  createdNodes: number;
  snappedNodes: number;
  removedOrphanNodes: number;
  createdSegmentFeatures: number;
  updatedSegmentAttributes: number;
  addedEdgeIds: number[];
  deletedEdgeIds: number[];
  createdNodeIds: number[];
  snappedNodeIds: number[];
}

export type RoadNetworkEditJobStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED";

export interface RoadNetworkEditJobResponse {
  jobId: number;
  status: RoadNetworkEditJobStatus;
  totalEdits: number;
  processedEdits: number;
  message: string;
  result?: RoadNetworkEditApplyResponse | null;
}

export type HazardReportStatus = "PENDING" | "APPROVED" | "REJECTED";

export type HazardReportType =
  | "STAIRS_STEP"
  | "BRAILLE_BLOCK"
  | "SIDEWALK_MISSING"
  | "RAMP"
  | "SIDEWALK_WIDTH"
  | "OTHER_OBSTACLE";

export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface AdminMeResponse {
  userId: string;
  role: AdminRole;
  permissions: string[];
}

export interface AdminHazardReportSummary {
  reportId: number;
  reporterUserId: string;
  reportType: HazardReportType;
  reportPoint: GeoPoint;
  status: HazardReportStatus;
  createdAt: string;
  representativeImageUrl: string | null;
}

export interface AdminHazardReportListResponse {
  content: AdminHazardReportSummary[];
  size: number;
  nextCursor: number | null;
  hasNext: boolean;
}

export interface AdminHazardReportDetail {
  reportId: number;
  reporterUserId: string;
  reportType: HazardReportType;
  description: string | null;
  reportPoint: GeoPoint;
  status: HazardReportStatus;
  createdAt: string;
  imageUrls: string[];
}

export interface AdminHazardReportStatusResponse {
  reportId: number;
  status: HazardReportStatus;
}
