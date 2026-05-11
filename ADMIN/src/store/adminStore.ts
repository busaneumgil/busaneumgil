import { create } from "zustand";
import type { AdminPage, AdminRole, Assignment, EditAction } from "../types";

const now = new Date().toISOString();
const draftStoragePrefix = "busan-eumgil-ADMIN:draft-edits:";

const initialAssignments: Assignment[] = [
  {
    assignmentId: "assign-gangseo-myeongji",
    gu: "강서구",
    dong: "명지동",
    assigneeId: "member-lee",
    assigneeName: "이재호",
    status: "DRAFT",
    changeSummary: { addedSideLine: 0, addedCrossWalk: 0, deletedSegments: 0, deletedNodes: 0 },
    assigneeMemo: "명지동 보행 네트워크 연결성 검수 중",
    adminMemo: "",
    updatedAt: now,
  },
  {
    assignmentId: "assign-gangseo-noksan",
    gu: "강서구",
    dong: "녹산동",
    assigneeId: "member-jang",
    assigneeName: "장주윤",
    status: "TODO",
    changeSummary: { addedSideLine: 0, addedCrossWalk: 0, deletedSegments: 0, deletedNodes: 0 },
    assigneeMemo: "",
    adminMemo: "",
    updatedAt: now,
  },
  {
    assignmentId: "assign-haeundae-u",
    gu: "해운대구",
    dong: "우동",
    assigneeId: "member-kim",
    assigneeName: "김응서",
    status: "REVIEW",
    changeSummary: { addedSideLine: 3, addedCrossWalk: 1, deletedSegments: 0, deletedNodes: 0 },
    assigneeMemo: "끊긴 SIDE_LINE 후보 3건 보강",
    adminMemo: "경로 테스트 후 반영 예정",
    updatedAt: now,
    reviewedAt: now,
  },
];

function draftStorageKey(assignmentId: string) {
  return `${draftStoragePrefix}${assignmentId}`;
}

function loadStoredDraftEdits(assignmentId: string): EditAction[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(draftStorageKey(assignmentId));
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function storeDraftEdits(assignmentId: string, edits: EditAction[]) {
  if (typeof window === "undefined") return;
  if (!edits.length) {
    window.localStorage.removeItem(draftStorageKey(assignmentId));
    return;
  }
  window.localStorage.setItem(draftStorageKey(assignmentId), JSON.stringify(edits));
}

interface AdminState {
  role: AdminRole;
  page: AdminPage;
  selectedAssignmentId: string;
  selectedGu: string;
  selectedDong: string;
  draftEdits: EditAction[];
  assignments: Assignment[];
  setPage: (page: AdminPage) => void;
  selectAssignment: (assignmentId: string) => void;
  setSelectedArea: (gu: string, dong: string) => void;
  addDraftEdit: (edit: EditAction) => void;
  undoDraftEdit: () => void;
  clearDraft: () => void;
  requestReview: () => void;
  markApplied: (assignmentId?: string) => void;
}

export const useAdminStore = create<AdminState>((set, get) => ({
  role: "ADMIN",
  page: "network",
  selectedAssignmentId: initialAssignments[0].assignmentId,
  selectedGu: initialAssignments[0].gu,
  selectedDong: initialAssignments[0].dong,
  draftEdits: loadStoredDraftEdits(initialAssignments[0].assignmentId),
  assignments: initialAssignments,
  setPage: (page) => set({ page }),
  selectAssignment: (assignmentId) => {
    const assignment = get().assignments.find((item) => item.assignmentId === assignmentId);
    if (!assignment) return;
    set({
      selectedAssignmentId: assignment.assignmentId,
      selectedGu: assignment.gu,
      selectedDong: assignment.dong,
      page: "network",
      draftEdits: loadStoredDraftEdits(assignment.assignmentId),
    });
  },
  setSelectedArea: (gu, dong) => {
    const matchingAssignment = get().assignments.find((assignment) => assignment.gu === gu && assignment.dong === dong);
    const selectedAssignmentId = matchingAssignment?.assignmentId ?? `area:${gu}:${dong}`;
    set({
      selectedAssignmentId,
      selectedGu: gu,
      selectedDong: dong,
      draftEdits: loadStoredDraftEdits(selectedAssignmentId),
    });
  },
  addDraftEdit: (edit) =>
    set((state) => {
      const draftEdits = [...state.draftEdits, edit];
      storeDraftEdits(state.selectedAssignmentId, draftEdits);
      return { draftEdits };
    }),
  undoDraftEdit: () =>
    set((state) => {
      const draftEdits = state.draftEdits.slice(0, -1);
      storeDraftEdits(state.selectedAssignmentId, draftEdits);
      return { draftEdits };
    }),
  clearDraft: () =>
    set((state) => {
      storeDraftEdits(state.selectedAssignmentId, []);
      return { draftEdits: [] };
    }),
  requestReview: () => {
    const selectedAssignmentId = get().selectedAssignmentId;
    set((state) => ({
      assignments: state.assignments.map((assignment) =>
        assignment.assignmentId === selectedAssignmentId
          ? { ...assignment, status: "REVIEW", updatedAt: new Date().toISOString() }
          : assignment,
      ),
    }));
  },
  markApplied: (assignmentId) => {
    const selectedAssignmentId = assignmentId ?? get().selectedAssignmentId;
    set((state) => ({
      assignments: state.assignments.map((assignment) =>
        assignment.assignmentId === selectedAssignmentId
          ? { ...assignment, status: "APPLIED", appliedAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
          : assignment,
      ),
      draftEdits: [],
    }));
    storeDraftEdits(selectedAssignmentId, []);
  },
}));
