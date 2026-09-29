import { api, ApiError, onSessionExpired } from "./api";
import { clearTokens, readTokens, writeTokens } from "./session";
import type { AuthResponse, LoginInput, RegisterInput, User } from "./types";

let user = $state<User | null>(null);
let ready = $state(false);
let restoreInFlight: Promise<void> | null = null;

function store(auth: AuthResponse): User {
    writeTokens({ token: auth.token, refreshToken: auth.refreshToken });
    user = auth.user;
    return auth.user;
}

async function runRestore(): Promise<void> {
    if (!readTokens()) {
        ready = true;
        return;
    }

    try {
        // The client transparently refreshes an expired access token first, so this
        // doubles as a validity check on whatever is sitting in storage.
        user = await api.get<User>("/api/auth/me");
    } catch {
        user = null;
        clearTokens();
    } finally {
        ready = true;
    }
}

function restore(): Promise<void> {
    // Every caller awaits the same attempt, so several components mounting at once
    // cannot race into duplicate /api/auth/me calls or read a half restored session.
    restoreInFlight ??= runRestore();
    return restoreInFlight;
}

onSessionExpired(() => {
    user = null;
});

export const auth = {
    get user(): User | null {
        return user;
    },
    get ready(): boolean {
        return ready;
    },
    get isAuthenticated(): boolean {
        return user !== null;
    },

    restore,

    async login(input: LoginInput): Promise<User> {
        return store(await api.post<AuthResponse>("/api/auth/login", input));
    },

    async register(input: RegisterInput): Promise<User> {
        return store(await api.post<AuthResponse>("/api/auth/register", input));
    },

    async logout(): Promise<void> {
        const tokens = readTokens();

        try {
            if (tokens) {
                await api.post<void>("/api/auth/logout", { refreshToken: tokens.refreshToken });
            }
        } catch (error) {
            // Revoking server side is best effort. The local session is cleared either
            // way, so a network failure must not leave the user stuck signed in.
            if (!(error instanceof ApiError)) console.warn("Logout request failed", error);
        } finally {
            clearTokens();
            user = null;
        }
    },
};
