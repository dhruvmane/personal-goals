import { clearTokens, readTokens, writeTokens } from "./session";
import type { AuthResponse } from "./types";

export const API_BASE_URL = import.meta.env.PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
    readonly status: number;
    readonly fieldErrors: Record<string, string>;

    constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

type SessionExpiredHandler = () => void;

let sessionExpired: SessionExpiredHandler = () => {};

export function onSessionExpired(handler: SessionExpiredHandler): void {
    sessionExpired = handler;
}

async function toApiError(response: Response): Promise<ApiError> {
    let message = `Request failed with status ${response.status}`;
    let fieldErrors: Record<string, string> = {};

    try {
        const body: unknown = await response.json();
        if (typeof body === "object" && body !== null) {
            if ("message" in body && typeof body.message === "string") message = body.message;
            if ("fieldErrors" in body && typeof body.fieldErrors === "object" && body.fieldErrors !== null) {
                fieldErrors = body.fieldErrors as Record<string, string>;
            }
        }
    } catch {
        // Non JSON error body, keep the status based message.
    }

    return new ApiError(response.status, message, fieldErrors);
}

let refreshInFlight: Promise<boolean> | null = null;

async function performRefresh(): Promise<boolean> {
    const tokens = readTokens();
    if (!tokens) return false;

    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ refreshToken: tokens.refreshToken }),
        });

        if (!response.ok) throw new Error("refresh rejected");

        const payload = (await response.json()) as AuthResponse;
        writeTokens({ token: payload.token, refreshToken: payload.refreshToken });
        return true;
    } catch {
        // The refresh token is spent, revoked, or expired. There is nothing left to
        // recover, so drop the session and let the UI route back to the login page.
        clearTokens();
        sessionExpired();
        return false;
    }
}

function refreshAccessToken(): Promise<boolean> {
    // Collapse concurrent 401s onto a single refresh call, otherwise a page that fires
    // several requests at once would rotate the token several times and trip reuse
    // detection on the server.
    refreshInFlight ??= performRefresh().finally(() => {
        refreshInFlight = null;
    });
    return refreshInFlight;
}

async function request<T>(path: string, init: RequestInit, canRetry = true): Promise<T> {
    const headers = new Headers(init.headers);
    const tokens = readTokens();

    if (tokens) headers.set("Authorization", `Bearer ${tokens.token}`);
    if (init.body) headers.set("Content-Type", "application/json");

    const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

    if (response.status === 401 && canRetry && tokens) {
        if (await refreshAccessToken()) {
            return request<T>(path, init, false);
        }
    }

    if (response.status === 204) return undefined as T;
    if (!response.ok) throw await toApiError(response);

    return (await response.json()) as T;
}

export const api = {
    get: <T>(path: string) => request<T>(path, { method: "GET" }),
    post: <T>(path: string, body?: unknown) =>
        request<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) }),
    put: <T>(path: string, body?: unknown) =>
        request<T>(path, { method: "PUT", body: body === undefined ? undefined : JSON.stringify(body) }),
    patch: <T>(path: string, body?: unknown) =>
        request<T>(path, { method: "PATCH", body: body === undefined ? undefined : JSON.stringify(body) }),
    delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
