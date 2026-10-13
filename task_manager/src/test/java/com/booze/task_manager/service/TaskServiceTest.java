package com.booze.task_manager.service;

import com.booze.task_manager.dto.Priority;
import com.booze.task_manager.dto.TaskRequest;
import com.booze.task_manager.exception.InvalidTaskFilterException;
import com.booze.task_manager.exception.TaskNotFoundException;
import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.TaskStatus;
import com.booze.task_manager.model.User;
import com.booze.task_manager.repo.TaskRepo;
import com.booze.task_manager.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepo taskRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private StatsCacheService statsCacheService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void listAppliesOwnerStatusHabitAndStableOneBasedPaging() {
        when(taskRepo.findTasks(eq("user-1"), eq(TaskStatus.TODO), eq("Reading"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        var result = taskService.list("user-1", 2, 5, "todo", " Reading ");

        assertEquals(2, result.page());
        assertEquals(5, result.size());
        assertEquals(0, result.total());
        assertEquals(0, result.totalPages());
        assertEquals(List.of(), result.items());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepo).findTasks(eq("user-1"), eq(TaskStatus.TODO), eq("Reading"), pageable.capture());
        assertEquals(5, pageable.getValue().getOffset());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(List.of("createdAt", "id"),
                pageable.getValue().getSort().stream().map(order -> order.getProperty()).toList());
        assertEquals(List.of(true, true),
                pageable.getValue().getSort().stream().map(order -> order.isDescending()).toList());
    }

    @Test
    void listIgnoresBlankFiltersAndReturnsEmptyPage() {
        when(taskRepo.findTasks(eq("user-1"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        var result = taskService.list("user-1", 1, 10, "  ", "\t");

        assertEquals(1, result.page());
        assertEquals(List.of(), result.items());
        verify(taskRepo).findTasks(eq("user-1"), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listRejectsUnknownStatusWithoutQueryingRepository() {
        assertThrows(InvalidTaskFilterException.class,
                () -> taskService.list("user-1", 1, 10, "unknown", "Reading"));

        verifyNoInteractions(taskRepo);
    }

    @Test
    void createDerivesOwnerAndInvalidatesStats() {
        User owner = new User();
        owner.setId("user-1");
        when(userRepo.findById("user-1")).thenReturn(Optional.of(owner));
        when(taskRepo.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest request = new TaskRequest(
                "Read a chapter", "", "Reading", LocalDate.of(2026, 10, 14), Priority.MEDIUM);
        Task saved = taskService.create("user-1", request);

        assertEquals(owner, saved.getOwner());
        assertEquals("Reading", saved.getHabit());
        assertEquals(LocalDate.of(2026, 10, 14), saved.getDueDate());
        verify(statsCacheService).invalidate("user-1");
    }

    @Test
    void completeUpdatesOnlyTheAuthenticatedUsersTaskAndInvalidatesStats() {
        Task completed = task("task-1", TaskStatus.DONE);
        when(taskRepo.completeIfNotDone(eq("task-1"), eq("user-1"), eq(TaskStatus.DONE), any(Instant.class)))
                .thenReturn(1);
        when(taskRepo.findOwnedTask("task-1", "user-1")).thenReturn(Optional.of(completed));

        TaskService.Completion result = taskService.complete("user-1", "task-1");

        assertEquals(completed, result.task());
        assertEquals(false, result.idempotent());
        verify(taskRepo).completeIfNotDone(eq("task-1"), eq("user-1"), eq(TaskStatus.DONE), any(Instant.class));
        verify(taskRepo).findOwnedTask("task-1", "user-1");
        verify(statsCacheService).invalidate("user-1");
    }

    @Test
    void duplicateCompletionDoesNotInvalidateStatsAgain() {
        Task completed = task("task-1", TaskStatus.DONE);
        when(taskRepo.completeIfNotDone(eq("task-1"), eq("user-1"), eq(TaskStatus.DONE), any(Instant.class)))
                .thenReturn(0);
        when(taskRepo.findOwnedTask("task-1", "user-1")).thenReturn(Optional.of(completed));

        TaskService.Completion result = taskService.complete("user-1", "task-1");

        assertEquals(true, result.idempotent());
        verify(statsCacheService, never()).invalidate(anyString());
    }

    @Test
    void completionOfAnotherUsersTaskIsReportedAsNotFound() {
        when(taskRepo.completeIfNotDone(eq("task-1"), eq("user-1"), eq(TaskStatus.DONE), any(Instant.class)))
                .thenReturn(0);
        when(taskRepo.findOwnedTask("task-1", "user-1")).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.complete("user-1", "task-1"));

        verify(taskRepo).completeIfNotDone(eq("task-1"), eq("user-1"), eq(TaskStatus.DONE), any(Instant.class));
        verify(taskRepo).findOwnedTask("task-1", "user-1");
        verifyNoInteractions(statsCacheService);
    }

    private static Task task(String id, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setStatus(status);
        return task;
    }
}
