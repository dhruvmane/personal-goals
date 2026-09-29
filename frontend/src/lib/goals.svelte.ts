import { api, ApiError } from "./api";
import { auth } from "./auth.svelte";
import type { Goal, GoalCounts, GoalInput, GoalPatch, GoalStatus } from "./types";

let goals = $state<Goal[]>([]);
let loading = $state(false);
let loaded = $state(false);
let signedIn = $state(false);
let loadError = $state("");
let busy = $state<Record<string, boolean>>({});
let loadInFlight: Promise<void> | null = null;

export const GOAL_STATUS_LABELS: Record<GoalStatus, string> = {
    PENDING: "To-Do",
    IN_PROGRESS: "In-Progress",
    COMPLETED: "Done",
    ARCHIVED: "Archived",
};

function describe(error: unknown): string {
    if (error instanceof ApiError) return error.message;
    return "Could not reach the server. Is the backend running?";
}

function markBusy(id: string, value: boolean): void {
    const next = { ...busy };
    if (value) next[id] = true;
    else delete next[id];
    busy = next;
}

function emptyCounts(): GoalCounts {
    return { PENDING: 0, IN_PROGRESS: 0, COMPLETED: 0, ARCHIVED: 0 };
}

function byDeadline(a: Goal, b: Goal): number {
    if (a.targetDate === b.targetDate) return a.createdAt.localeCompare(b.createdAt);
    if (a.targetDate === null) return 1;
    if (b.targetDate === null) return -1;
    return a.targetDate.localeCompare(b.targetDate);
}

async function runLoad(): Promise<void> {
    loading = true;
    loadError = "";

    try {
        // The session has to be settled before goals are requested, otherwise a cold
        // page load would fire an unauthenticated GET and bounce the user to login.
        await auth.restore();
        signedIn = auth.isAuthenticated;

        if (!signedIn) {
            goals = [];
            return;
        }

        goals = await api.get<Goal[]>("/api/goals");
    } catch (error) {
        goals = [];
        loadError = describe(error);
    } finally {
        loading = false;
        loaded = true;
    }
}

export const goalStore = {
    get goals(): Goal[] {
        return goals;
    },
    get loading(): boolean {
        return loading;
    },
    get loaded(): boolean {
        return loaded;
    },
    get signedIn(): boolean {
        return signedIn;
    },
    get loadError(): string {
        return loadError;
    },

    get counts(): GoalCounts {
        const counts = emptyCounts();
        for (const goal of goals) counts[goal.status] += 1;
        return counts;
    },

    get total(): number {
        return goals.length;
    },

    get completed(): number {
        return goals.filter((goal) => goal.status === "COMPLETED").length;
    },

    get active(): number {
        return this.counts.PENDING + this.counts.IN_PROGRESS;
    },

    /** Share of all goals that are done, as a whole percentage. */
    get completionRate(): number {
        return goals.length === 0 ? 0 : Math.round((this.completed / goals.length) * 100);
    },

    get averageProgress(): number {
        if (goals.length === 0) return 0;
        const total = goals.reduce((sum, goal) => sum + goal.progress, 0);
        return Math.round(total / goals.length);
    },

    /** Goals that carry a deadline, soonest first. */
    get withDeadline(): Goal[] {
        return goals.filter((goal) => goal.targetDate !== null).sort(byDeadline);
    },

    byStatus(status: GoalStatus): Goal[] {
        return goals.filter((goal) => goal.status === status);
    },

    isBusy(id: string): boolean {
        return busy[id] === true;
    },

    /** Loads once per page. Pass `true` to refetch after an error or a session change. */
    load(force = false): Promise<void> {
        if (force) loadInFlight = null;
        loadInFlight ??= runLoad();
        return loadInFlight;
    },

    async create(input: GoalInput): Promise<Goal> {
        const created = await api.post<Goal>("/api/goals", {
            title: input.title.trim(),
            description: input.description?.trim() || null,
            status: input.status ?? "PENDING",
            progress: input.progress ?? 0,
            targetDate: input.targetDate || null,
        });

        goals = [created, ...goals];
        return created;
    },

    async update(id: string, patch: GoalPatch): Promise<Goal> {
        markBusy(id, true);
        try {
            const updated = await api.put<Goal>(`/api/goals/${id}`, patch);
            goals = goals.map((goal) => (goal.id === id ? updated : goal));
            return updated;
        } finally {
            markBusy(id, false);
        }
    },

    async remove(id: string): Promise<void> {
        markBusy(id, true);
        // Drop the card immediately and put it back if the delete is rejected, so the
        // grid never flashes a goal that the server still holds.
        const previous = goals;
        goals = goals.filter((goal) => goal.id !== id);

        try {
            await api.delete<void>(`/api/goals/${id}`);
        } catch (error) {
            goals = previous;
            throw error;
        } finally {
            markBusy(id, false);
        }
    },

    describe,
};
