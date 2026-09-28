package com.personalgoals.goal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record GoalUpdateRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 5000) String description,
        GoalStatus status,
        @Min(0) @Max(100) Integer progress,
        LocalDate targetDate
) {
    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public boolean hasDescription() {
        return description != null;
    }

    public boolean hasStatus() {
        return status != null;
    }

    public boolean hasProgress() {
        return progress != null;
    }

    public boolean hasTargetDate() {
        return targetDate != null;
    }
}
