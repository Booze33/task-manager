package com.booze.task_manager.exception;

public class InvalidTaskFilterException extends RuntimeException {
    public InvalidTaskFilterException(String status) {
        super("Unsupported task status: " + status);
    }
}
