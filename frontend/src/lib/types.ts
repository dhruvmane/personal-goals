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
