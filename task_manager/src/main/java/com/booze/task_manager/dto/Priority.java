package com.booze.task_manager.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Priority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    Priority(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Priority fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
