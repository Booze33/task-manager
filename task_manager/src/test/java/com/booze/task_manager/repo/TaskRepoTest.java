package com.booze.task_manager.repo;

import com.booze.task_manager.dto.Priority;
import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.TaskStatus;
import com.booze.task_manager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TaskRepoTest {
    @Autowired
    private TaskRepo taskRepo;

    @Autowired
    private UserRepo userRepo;

    private User owner;
    private User otherUser;

    @BeforeEach
    void createUsersAndTasks() {
        owner = user("owner@example.com");
        otherUser = user("other@example.com");

        task("Read", owner, TaskStatus.TODO, "Reading", Instant.parse("2026-10-13T07:00:00Z"));
        task("Write", owner, TaskStatus.DONE, "Writing", Instant.parse("2026-10-12T07:00:00Z"));
        task("Private task", otherUser, TaskStatus.TODO, "Reading", Instant.parse("2026-10-14T07:00:00Z"));
        taskRepo.flush();
    }

    @Test
    void queryFiltersByOwnerStatusAndExactHabitBeforePaging() {
        Page<Task> result = taskRepo.findTasks(
                owner.getId(),
                TaskStatus.TODO,
                "Reading",
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        assertEquals(1, result.getTotalElements());
        assertEquals("Read", result.getContent().get(0).getTitle());
    }

    @Test
    void unknownHabitAndOutOfRangePageReturnEmptyPage() {
        Page<Task> result = taskRepo.findTasks(
                owner.getId(),
                null,
                "Unknown",
                PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getContent().size());
    }

    @Test
    void completionIsAtomicAndCannotChangeAnotherUsersTask() {
        Task ownedTask = taskRepo.findTasks(
                owner.getId(), null, null, PageRequest.of(0, 10)).getContent().get(0);

        assertEquals(1, taskRepo.completeIfNotDone(
                ownedTask.getId(), owner.getId(), TaskStatus.DONE, Instant.parse("2026-10-13T08:00:00Z")));
        assertEquals(0, taskRepo.completeIfNotDone(
                ownedTask.getId(), owner.getId(), TaskStatus.DONE, Instant.parse("2026-10-13T09:00:00Z")));
        assertEquals(0, taskRepo.completeIfNotDone(
                ownedTask.getId(), otherUser.getId(), TaskStatus.DONE, Instant.parse("2026-10-13T10:00:00Z")));

        Task saved = taskRepo.findOwnedTask(ownedTask.getId(), owner.getId()).orElseThrow();
        assertEquals(TaskStatus.DONE, saved.getStatus());
        assertEquals(Instant.parse("2026-10-13T08:00:00Z"), saved.getCompletedAt());
    }

    private User user(String email) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        return userRepo.saveAndFlush(user);
    }

    private void task(String title, User taskOwner, TaskStatus status, String habit, Instant createdAt) {
        Task task = new Task();
        task.setOwner(taskOwner);
        task.setTitle(title);
        task.setDescription("");
        task.setStatus(status);
        task.setHabit(habit);
        task.setDueDate(LocalDate.of(2026, 10, 14));
        task.setCreatedAt(createdAt);
        task.setPriority(Priority.MEDIUM);
        taskRepo.save(task);
    }
}
