package com.personalgoals.goal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record GoalCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        GoalStatus status,
        @Min(0) @Max(100) Integer progress,
        LocalDate targetDate
) {
    public GoalStatus statusOrDefault() {
        return status == null ? GoalStatus.PENDING : status;
    }

    public int progressOrDefault() {
        return progress == null ? 0 : progress;
    }
}
