package com.booze.task_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank
        @Size(min = 3, max = 200)
        String title,

        @Size(max = 2000)
        String description,

        @NotBlank
        @Size(max = 100)
        String habit,

        @NotNull
        LocalDate dueDate,

        @NotNull
        Priority priority
) {
}
