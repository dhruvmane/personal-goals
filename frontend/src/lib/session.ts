export type Tokens = {
    token: string;
    refreshToken: string;
};

const STORAGE_KEY = "goalden.session";

// Components should treat these as opaque: read them through the api client rather than
// sending them by hand, so an expired access token is refreshed instead of being reused.
export function readTokens(): Tokens | null {
    if (typeof localStorage === "undefined") return null;

    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;

    try {
        const parsed: unknown = JSON.parse(raw);
        if (
            typeof parsed === "object" &&
            parsed !== null &&
            "token" in parsed &&
            typeof parsed.token === "string" &&
            "refreshToken" in parsed &&
            typeof parsed.refreshToken === "string"
        ) {
            return { token: parsed.token, refreshToken: parsed.refreshToken };
        }
    } catch {
        // Corrupt payload, treat it as no session at all.
    }

    clearTokens();
    return null;
}

export function writeTokens(tokens: Tokens): void {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
}

export function clearTokens(): void {
    if (typeof localStorage === "undefined") return;
    localStorage.removeItem(STORAGE_KEY);
}
