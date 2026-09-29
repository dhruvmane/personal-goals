export type Role = "USER" | "ADMIN";

export type User = {
    id: string;
    email: string;
    displayName: string | null;
    role: Role;
    createdAt: string;
    updatedAt: string;
};

export type AuthResponse = {
    token: string;
    tokenType: string;
    expiresIn: number;
    refreshToken: string;
    refreshExpiresIn: number;
    user: User;
};

export type RegisterInput = {
    email: string;
    password: string;
    displayName?: string;
};

export type LoginInput = {
    email: string;
    password: string;
};

export type GoalStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "ARCHIVED";

export const GOAL_STATUSES: GoalStatus[] = ["PENDING", "IN_PROGRESS", "COMPLETED", "ARCHIVED"];

export type Goal = {
    id: string;
    userId: string;
    title: string;
    description: string | null;
    status: GoalStatus;
    progress: number;
    /** `YYYY-MM-DD`, or null when the goal has no deadline. */
    targetDate: string | null;
    createdAt: string;
    updatedAt: string;
};

export type GoalInput = {
    title: string;
    description?: string | null;
    status?: GoalStatus;
    progress?: number;
    targetDate?: string | null;
};

export type GoalPatch = Pick<GoalInput, "status" | "progress">;

export type GoalCounts = Record<GoalStatus, number>;
