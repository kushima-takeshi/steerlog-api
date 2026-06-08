package com.steerlog.exception;

public class SessionAlreadyInProgressException extends RuntimeException {

    public SessionAlreadyInProgressException(String message) {
        super(message);
    }
}
