package com.steerlog.exception;

import com.steerlog.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ProgressNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProgressNotFound(ProgressNotFoundException ex) {
        ErrorResponse body = new ErrorResponse("PROGRESS_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(LevelRequirementNotMetException.class)
    public ResponseEntity<ErrorResponse> handleLevelRequirementNotMet(LevelRequirementNotMetException ex) {
        ErrorResponse body = new ErrorResponse("LEVEL_REQUIREMENT_NOT_MET", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(SessionAlreadyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleSessionAlreadyInProgress(SessionAlreadyInProgressException ex) {
        ErrorResponse body = new ErrorResponse("SESSION_ALREADY_IN_PROGRESS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
