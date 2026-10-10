package com.booze.task_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TaskRequest(
        @NotNull
        @Positive
        String user_id,

        @NotBlank
        String title,

        @NotBlank
        String description,

        @NotNull
        Priority priority
) {
}
