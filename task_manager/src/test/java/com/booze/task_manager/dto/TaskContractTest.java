package com.booze.task_manager.dto;

import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void taskResponseUsesTheClientContract() {
        Task task = new Task();
        task.setId("task-id");
        task.setTitle("Read");
        task.setDescription("");
        task.setStatus(TaskStatus.TODO);
        task.setHabit("Reading");
        task.setDueDate(LocalDate.of(2026, 10, 14));
        task.setCreatedAt(Instant.parse("2026-10-13T07:00:00Z"));
        task.setPriority(Priority.HIGH);

        JsonNode response = objectMapper.valueToTree(TaskResponse.from(task));
        Set<String> expectedFields = Set.of(
                "id", "title", "description", "status", "habit", "dueDate", "createdAt", "priority");

        assertEquals(expectedFields.size(), response.size());
        expectedFields.forEach(field -> assertTrue(response.has(field), "missing field: " + field));
        assertEquals("todo", response.get("status").asText());
        assertEquals("high", response.get("priority").asText());
        assertEquals("2026-10-14", response.get("dueDate").asText());
        assertEquals("2026-10-13T07:00:00Z", response.get("createdAt").asText());
    }

    @Test
    void taskRequestAcceptsLowercasePriority() throws Exception {
        TaskRequest request = objectMapper.readValue("""
                {
                  "title": "Read",
                  "description": "",
                  "habit": "Reading",
                  "dueDate": "2026-10-14",
                  "priority": "high"
                }
                """, TaskRequest.class);

        assertEquals(Priority.HIGH, request.priority());
        assertEquals(LocalDate.of(2026, 10, 14), request.dueDate());
    }
}
