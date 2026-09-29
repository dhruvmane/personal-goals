const MS_PER_DAY = 86_400_000;

function parseTargetDate(target: string): Date {
    // The API sends a bare `YYYY-MM-DD`. Pinning the time to local noon keeps the day
    // stable regardless of the viewer's timezone, which `new Date("2026-09-13")` does not.
    const [year, month, day] = target.split("-").map(Number);
    return new Date(year, month - 1, day, 12, 0, 0, 0);
}

function startOfToday(): Date {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate(), 12, 0, 0, 0);
}

export function formatTargetDate(target: string | null): string {
    if (!target) return "No deadline";
    return parseTargetDate(target).toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric",
    });
}

/** Negative once the deadline has passed. */
export function daysUntil(target: string): number {
    return Math.round((parseTargetDate(target).getTime() - startOfToday().getTime()) / MS_PER_DAY);
}

export function formatDeadlineHint(target: string | null): string {
    if (!target) return "No deadline";

    const days = daysUntil(target);
    if (days === 0) return "Due today";
    if (days === 1) return "Due tomorrow";
    if (days === -1) return "1 day overdue";
    if (days < 0) return `${Math.abs(days)} days overdue`;
    return `in ${days} days`;
}
