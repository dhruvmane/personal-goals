package com.personalgoals.goal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID userId,
        String title,
        String description,
        GoalStatus status,
        int progress,
        LocalDate targetDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static GoalResponse from(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getUserId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getStatus(),
                goal.getProgress(),
                goal.getTargetDate(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
