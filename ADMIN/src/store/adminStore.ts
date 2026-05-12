import { create } from "zustand";
import type { AdminPage, AdminRole, EditAction } from "../types";

const draftStoragePrefix = "busan-eumgil-ADMIN:draft-edits:";
const defaultGu = "강서구";
const defaultDong = "명지동";

function areaAssignmentId(gu: string, dong: string) {
  return `area:${gu}:${dong}`;
}

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
  setPage: (page: AdminPage) => void;
  setSelectedArea: (gu: string, dong: string) => void;
  addDraftEdit: (edit: EditAction) => void;
  undoDraftEdit: () => void;
  clearDraft: () => void;
  clearDraftForAssignment: (assignmentId?: string) => void;
}

const initialAssignmentId = areaAssignmentId(defaultGu, defaultDong);

export const useAdminStore = create<AdminState>((set) => ({
  role: "ADMIN",
  page: "network",
  selectedAssignmentId: initialAssignmentId,
  selectedGu: defaultGu,
  selectedDong: defaultDong,
  draftEdits: loadStoredDraftEdits(initialAssignmentId),
  setPage: (page) => set({ page }),
  setSelectedArea: (gu, dong) => {
    const selectedAssignmentId = areaAssignmentId(gu, dong);
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
  clearDraftForAssignment: (assignmentId) =>
    set((state) => {
      const targetAssignmentId = assignmentId ?? state.selectedAssignmentId;
      storeDraftEdits(targetAssignmentId, []);
      if (targetAssignmentId === state.selectedAssignmentId) {
        return { draftEdits: [] };
      }
      return {};
    }),
}));
