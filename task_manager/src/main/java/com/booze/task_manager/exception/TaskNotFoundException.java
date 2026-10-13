package com.booze.task_manager.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException() {
        super("Task not found");
    }
}
