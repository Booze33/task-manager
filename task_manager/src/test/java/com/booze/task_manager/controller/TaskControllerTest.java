package com.booze.task_manager.controller;

import com.booze.task_manager.dto.Priority;
import com.booze.task_manager.dto.TaskPageResponse;
import com.booze.task_manager.dto.TaskRequest;
import com.booze.task_manager.dto.TaskResponse;
import com.booze.task_manager.exception.GlobalExceptionHandler;
import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.TaskStatus;
import com.booze.task_manager.model.User;
import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {
    private final TaskService taskService = mock(TaskService.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        User user = new User();
        user.setId("user-1");
        user.setName("Alex");
        user.setEmail("alex@example.com");
        user.setRole(com.booze.task_manager.model.Role.USER);
        var principal = UserPrincipal.forToken(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void listRouteReturnsPageContractAndPassesFilters() throws Exception {
        TaskResponse task = response();
        when(taskService.list("user-1", 2, 5, "todo", "Reading"))
                .thenReturn(new TaskPageResponse(List.of(task), 1, 2, 5, 1));

        mvc.perform(get("/api/tasks").param("page", "2").param("size", "5")
                        .param("status", "todo").param("habit", "Reading"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("task-1"))
                .andExpect(jsonPath("$.items[0].status").value("todo"))
                .andExpect(jsonPath("$.items[0].priority").value("high"))
                .andExpect(jsonPath("$.items[0].habit").value("Reading"))
                .andExpect(jsonPath("$.items[0].dueDate").value("2026-10-14"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(taskService).list("user-1", 2, 5, "todo", "Reading");
    }

    @Test
    void createRouteValidatesRequestAndReturnsCreatedTask() throws Exception {
        Task task = task();
        when(taskService.create(eq("user-1"), any(TaskRequest.class))).thenReturn(task);

        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Read a chapter","description":"","habit":"Reading",
                         "dueDate":"2026-10-14","priority":"high"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/task-1"))
                .andExpect(jsonPath("$.id").value("task-1"))
                .andExpect(jsonPath("$.priority").value("high"))
                .andExpect(jsonPath("$.status").value("todo"));

        verify(taskService).create(eq("user-1"), any(TaskRequest.class));
    }

    @Test
    void invalidCreateRequestReturnsFieldValidationErrors() throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":" ","description":"","habit":"Reading",
                         "dueDate":"2026-10-14","priority":"high"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verifyNoInteractions(taskService);
    }

    @Test
    void completeRouteReturnsTaskAndIdempotencyHeader() throws Exception {
        when(taskService.complete("user-1", "task-1"))
                .thenReturn(new TaskService.Completion(task(), true));

        mvc.perform(post("/api/tasks/task-1/complete"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotent", "true"))
                .andExpect(jsonPath("$.id").value("task-1"))
                .andExpect(jsonPath("$.status").value("todo"));
    }

    private static TaskResponse response() {
        return TaskResponse.from(task());
    }

    private static Task task() {
        Task task = new Task();
        task.setId("task-1");
        task.setTitle("Read a chapter");
        task.setDescription("");
        task.setStatus(TaskStatus.TODO);
        task.setHabit("Reading");
        task.setDueDate(LocalDate.of(2026, 10, 14));
        task.setCreatedAt(Instant.parse("2026-10-13T07:00:00Z"));
        task.setPriority(Priority.HIGH);
        return task;
    }
}
