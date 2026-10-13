package com.booze.task_manager.model;

public enum TaskStatus {
    TODO("todo"),
    IN_PROGRESS("in-progress"),
    DONE("done");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported task status: " + value);
    }
}
