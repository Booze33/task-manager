package com.booze.task_manager.exception;

public class InvalidRefreshTokenException extends RuntimeException{
    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
