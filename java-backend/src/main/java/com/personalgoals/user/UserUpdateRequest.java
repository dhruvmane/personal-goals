package com.personalgoals.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Email @Size(max = 320) String email,
        @Size(max = 120) String displayName,
        @Size(min = 8, max = 72) String password,
        Role role
) {
    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    public boolean hasDisplayName() {
        return displayName != null;
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    public boolean hasRole() {
        return role != null;
    }
}
