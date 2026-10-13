package com.booze.task_manager.controller;

import com.booze.task_manager.dto.UserResponse;
import com.booze.task_manager.dto.UserStatsResponse;
import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserStatsService userStatsService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    @GetMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> stats(@AuthenticationPrincipal UserPrincipal user) {
        var cached = userStatsService.get(user.getId());
        return ResponseEntity.ok()
                .header("X-Cache", cached.hit() ? "HIT" : "MISS")
                .body(cached.stats());
    }

    @DeleteMapping("/me/stats/cache")
    public ResponseEntity<Void> invalidateStats(@AuthenticationPrincipal UserPrincipal user) {
        userStatsService.invalidate(user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/greet")
    public String greet() {
        return "testing controllers";
    }
}
