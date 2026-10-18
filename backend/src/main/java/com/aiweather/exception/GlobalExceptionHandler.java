package com.aiweather.exception;

import com.aiweather.dto.ApiDtos.ErrorResponse;
import com.aiweather.dto.ApiDtos.FieldIssue;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<ErrorResponse> build(HttpStatus s, String code, String msg, List<FieldIssue> details) {
        return ResponseEntity.status(s).body(new ErrorResponse(Instant.now(), s.value(), code, msg, details));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e) {
        if (e.status().is5xxServerError()) log.warn("{}: {}", e.code(), e.getMessage());
        return build(e.status(), e.code(), e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        List<FieldIssue> issues = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldIssue(f.getField(), f.getDefaultMessage())).toList();
        return build(HttpStatus.BAD_REQUEST, "invalid_input", "The request is not valid.", issues);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missing(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST, "invalid_input", "Missing required parameter '" + e.getParameterName() + "'.", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> mismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "invalid_input", "Parameter '" + e.getName() + "' has an invalid value.", List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "invalid_input", "The request body is missing or malformed (check dates and enum values).", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "conflict", "The data conflicts with an existing record.", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "not_found", "No such endpoint.", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> other(Exception e) {
        log.error("Unhandled error", e); // stack trace stays in the log; clients get a generic message
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Something went wrong on the server.", List.of());
    }
}
