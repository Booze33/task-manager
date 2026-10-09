package com.booze.task_manager.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("Unable to create account");
    }
}
