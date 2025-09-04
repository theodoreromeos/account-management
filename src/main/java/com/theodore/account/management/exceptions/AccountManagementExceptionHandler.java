package com.theodore.account.management.exceptions;

import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.racingmodel.models.MobilityAppErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class AccountManagementExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountManagementExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        LOGGER.warn("Validation failed [{}]: {}", ex.getBindingResult().getObjectName(), fieldErrors, ex);

        String userMessage = getExceptionMessage(ex.getBindingResult(), "Bad Request");

        MobilityAppErrorResponse error = new MobilityAppErrorResponse(userMessage, Instant.now());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleTokenExpiredErrors(ExpiredJwtException ex) {
        LOGGER.warn("JWT expired at {}: {}", ex.getClaims().getExpiration(), ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Expired token", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleInvalidTokenErrors(JwtException ex) {
        LOGGER.error("Invalid JWT token: {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Invalid token", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleNotFoundErrors(NotFoundException ex) {
        LOGGER.warn("Resource not found: {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse(ex.getMessage(), Instant.now());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccountConfirmationException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleAccountConfirmationException(AccountConfirmationException ex) {
        LOGGER.error("Account confirmation failed: {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Account confirmation failed", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleInvalidStatusException(InvalidStatusException ex) {
        LOGGER.error("Invalid Status : {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Invalid Status", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        LOGGER.error("{}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse(ex.getMessage(), Instant.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
    public ResponseEntity<MobilityAppErrorResponse> handleAuthorizationDeniedException(RuntimeException ex) {
        LOGGER.error("Invalid Permissions: {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Invalid Permissions", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<MobilityAppErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        LOGGER.error("Data Integrity error : {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Data Integrity error", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MobilityAppErrorResponse> handleRuntimeException(RuntimeException ex) {
        LOGGER.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Unexpected error occurred", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MobilityAppErrorResponse> handleGeneral(Exception ex) {
        LOGGER.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        MobilityAppErrorResponse error = new MobilityAppErrorResponse("Unexpected error occurred", Instant.now());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getExceptionMessage(BindingResult bindingResult, String alternativeMessage) {
        return bindingResult
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList()
                .stream().findFirst().orElse(alternativeMessage);
    }


}
