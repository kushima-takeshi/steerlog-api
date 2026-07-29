package com.steerlog.exception;

public class InvalidProgressStatusTransitionException extends RuntimeException {

    public InvalidProgressStatusTransitionException(String message) {
        super(message);
    }
}
