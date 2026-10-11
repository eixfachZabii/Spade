package com.spade.api;

import com.spade.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * One error envelope for every endpoint: RFC 9457 problem details (#2: raw exception messages reached
 * clients, some errors returned 200). Spring MVC's own errors (404, validation, bad JSON) come from the parent.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * A rejected request body says which field failed and how to fix it. Never the rejected value:
     * one of the fields can be a password.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = e.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        String detail = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField).thenComparing(
                        error -> String.valueOf(error.getDefaultMessage())))
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (!detail.isEmpty()) {
            body.setDetail(detail);
        }
        return handleExceptionInternal(e, body, headers, status, request);
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail domainRule(DomainException e) {
        return ProblemDetail.forStatusAndDetail(status(e.kind()), e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail clash(DataIntegrityViolationException e) {
        log.info("database refused a write: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "That clashes with something that already exists. Reload and try again.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side.");
    }

    static HttpStatus status(DomainException.Kind kind) {
        return switch (kind) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
        };
    }
}
