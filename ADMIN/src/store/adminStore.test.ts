import { afterEach, describe, expect, it, vi } from "vitest";
import { useAdminStore } from "./adminStore";
import type { AdminPage } from "../types";

describe("adminStore role model", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("keeps ADMIN authority limited to ADMIN", () => {
    expect(useAdminStore.getState().role).toBe("ADMIN");
  });

  it("starts from the real editing workspace instead of a placeholder dashboard", () => {
    expect(useAdminStore.getState().page).toBe("network");
  });

  it("models second MVP workspaces as task tabs", () => {
    const pages: AdminPage[] = ["network", "facilities", "hazards", "users", "logs"];

    expect(pages).toEqual(["network", "facilities", "hazards", "users", "logs"]);
  });

  it("persists draft edits locally until they are cleared or applied", () => {
    const storage = new Map<string, string>();
    vi.stubGlobal("window", {
      localStorage: {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => storage.set(key, value),
        removeItem: (key: string) => storage.delete(key),
      },
    });

    useAdminStore.getState().clearDraft();
    useAdminStore.getState().addDraftEdit({
      action: "add_segment",
      segmentType: "SIDE_LINE",
      geom: { type: "LineString", coordinates: [[129, 35], [129.1, 35.1]] },
    });

    const stored = [...storage.values()][0];
    expect(stored).toContain("add_segment");

    useAdminStore.getState().clearDraft();
    expect(storage.size).toBe(0);
  });

  it("scopes drafts to the selected gu and dong instead of carrying edits across areas", () => {
    const storage = new Map<string, string>();
    vi.stubGlobal("window", {
      localStorage: {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => storage.set(key, value),
        removeItem: (key: string) => storage.delete(key),
      },
    });

    useAdminStore.getState().setSelectedArea("강서구", "명지동");
    useAdminStore.getState().clearDraft();
    useAdminStore.getState().addDraftEdit({
      action: "delete_segment",
      edgeId: "old-area-edge",
    });

    useAdminStore.getState().setSelectedArea("북구", "덕천동");

    expect(useAdminStore.getState().selectedAssignmentId).toBe("area:북구:덕천동");
    expect(useAdminStore.getState().draftEdits).toEqual([]);
  });
});
