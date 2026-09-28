package com.personalgoals.goal;

import com.personalgoals.user.User;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalResponse> list(@AuthenticationPrincipal User current) {
        return goalService.list(current.getId());
    }

    @GetMapping("/stats")
    public Map<GoalStatus, Long> stats(@AuthenticationPrincipal User current) {
        return goalService.stats(current.getId());
    }

    @GetMapping("/{id}")
    public GoalResponse get(@AuthenticationPrincipal User current, @PathVariable UUID id) {
        return goalService.get(current.getId(), id);
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(
            @AuthenticationPrincipal User current,
            @Valid @RequestBody GoalCreateRequest request) {
        GoalResponse created = goalService.create(current.getId(), request);
        return ResponseEntity
                .created(URI.create("/api/goals/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public GoalResponse update(
            @AuthenticationPrincipal User current,
            @PathVariable UUID id,
            @Valid @RequestBody GoalUpdateRequest request) {
        return goalService.update(current.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User current, @PathVariable UUID id) {
        goalService.delete(current.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
