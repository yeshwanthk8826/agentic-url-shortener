package com.yeshwanthk.agentic_url_shortener.exception;

import com.yeshwanthk.agentic_url_shortener.idempotency.exception.IdempotencyConflictException;
import com.yeshwanthk.agentic_url_shortener.idempotency.exception.IdempotencyInProgressException;
import com.yeshwanthk.agentic_url_shortener.idempotency.exception.IdempotencySerializationException;
import com.yeshwanthk.agentic_url_shortener.orchestration.exception.WorkflowNotFoundException;
import com.yeshwanthk.agentic_url_shortener.orchestration.exception.WorkflowStateException;
import com.yeshwanthk.agentic_url_shortener.url.exception.InvalidUrlException;
import com.yeshwanthk.agentic_url_shortener.url.exception.ShortCodeGenerationException;
import com.yeshwanthk.agentic_url_shortener.url.exception.ShortUrlNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ProblemDetail handleNotFound(
            ShortUrlNotFoundException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "Short URL not found",
                exception.getMessage(),
                "urn:problem:short-url-not-found",
                request
        );
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ProblemDetail handleInvalidUrl(
            InvalidUrlException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid URL",
                exception.getMessage(),
                "urn:problem:invalid-url",
                request
        );
    }

    @ExceptionHandler(ShortCodeGenerationException.class)
    public ProblemDetail handleCodeGenerationFailure(
            ShortCodeGenerationException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Short-code allocation unavailable",
                exception.getMessage(),
                "urn:problem:short-code-unavailable",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleRequestValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "One or more request fields are invalid",
                "urn:problem:request-validation",
                request
        );

        List<Map<String, String>> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null
                                ? "Invalid value"
                                : error.getDefaultMessage()
                ))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "One or more request parameters are invalid",
                "urn:problem:constraint-validation",
                request
        );

        List<Map<String, String>> errors = exception
                .getConstraintViolations()
                .stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()
                ))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            String type,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(type));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(
            IdempotencyConflictException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Idempotency key conflict",
                exception.getMessage(),
                "urn:problem:idempotency-conflict",
                request
        );
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ProblemDetail handleIdempotencyInProgress(
            IdempotencyInProgressException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.CONFLICT,
                "Idempotent request in progress",
                exception.getMessage(),
                "urn:problem:idempotency-in-progress",
                request
        );

        problem.setProperty("retryable", true);
        return problem;
    }

    @ExceptionHandler(IdempotencySerializationException.class)
    public ProblemDetail handleIdempotencySerialization(
            IdempotencySerializationException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Idempotency processing failed",
                "The idempotent response could not be processed",
                "urn:problem:idempotency-processing",
                request
        );
    }

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ProblemDetail handleWorkflowNotFound(
            WorkflowNotFoundException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "Workflow not found",
                exception.getMessage(),
                "urn:problem:workflow-not-found",
                request
        );
    }

    @ExceptionHandler(WorkflowStateException.class)
    public ProblemDetail handleWorkflowState(
            WorkflowStateException exception,
            HttpServletRequest request
    ) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Workflow state conflict",
                exception.getMessage(),
                "urn:problem:workflow-state-conflict",
                request
        );
    }
}