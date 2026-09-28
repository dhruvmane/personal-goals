package com.personalgoals.goal;

import com.personalgoals.common.NotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public List<GoalResponse> list(UUID userId) {
        return goalRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(GoalResponse::from)
                .toList();
    }

    public GoalResponse get(UUID userId, UUID goalId) {
        return GoalResponse.from(requireOwned(userId, goalId));
    }

    @Transactional
    public GoalResponse create(UUID userId, GoalCreateRequest request) {
        Goal goal = new Goal(
                userId,
                request.title().trim(),
                normalize(request.description()),
                request.statusOrDefault(),
                request.progressOrDefault(),
                request.targetDate()
        );
        return GoalResponse.from(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse update(UUID userId, UUID goalId, GoalUpdateRequest request) {
        Goal goal = requireOwned(userId, goalId);

        if (request.hasTitle()) {
            goal.changeTitle(request.title().trim());
        }
        if (request.hasDescription()) {
            goal.changeDescription(normalize(request.description()));
        }
        if (request.hasStatus()) {
            goal.changeStatus(request.status());
        }
        if (request.hasProgress()) {
            goal.changeProgress(request.progress());
        }
        if (request.hasTargetDate()) {
            goal.changeTargetDate(request.targetDate());
        }

        return GoalResponse.from(goal);
    }

    @Transactional
    public void delete(UUID userId, UUID goalId) {
        goalRepository.delete(requireOwned(userId, goalId));
    }

    public Map<GoalStatus, Long> stats(UUID userId) {
        Map<GoalStatus, Long> counts = new EnumMap<>(GoalStatus.class);
        for (GoalStatus status : GoalStatus.values()) {
            counts.put(status, goalRepository.countByUserIdAndStatus(userId, status));
        }
        return counts;
    }

    private Goal requireOwned(UUID userId, UUID goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> NotFoundException.of("Goal", goalId));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
