package com.booze.task_manager.controller;

import com.booze.task_manager.dto.TaskPageResponse;
import com.booze.task_manager.dto.TaskRequest;
import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public TaskPageResponse list(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
        @RequestParam(required = false) String status
    ) {
        return taskService.list(user.getId(), page, size, status);
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@AuthenticationPrincipal UserPrincipal user,
                                           @RequestBody TaskRequest request) {
        Task task = taskService.create(user.getId(), request);

        return ResponseEntity.created(URI.create("/api/tasks/" + task.getId())).body(task);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Task> complete(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String id
    ) {
        TaskService.Completion result = taskService.complete(user.getId(), id);

        return ResponseEntity.ok()
                .header("X-idempotent", Boolean.toString(result.idempotent()))
                .body(result.task());
    }
}
