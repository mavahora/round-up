package com.starling.roundup.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starling.roundup.model.response.ErrorResponse;
import com.starling.roundup.model.response.StarlingError;
import com.starling.roundup.model.response.StarlingErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation failed: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
                log.error("Field: {} - Error: {}", error.getField(), error.getDefaultMessage());
                errors.put(error.getField(), error.getDefaultMessage());
            });
        return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed: " + errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument exception: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException ex) {
        log.error("Insufficient funds for transfer: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse("Unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(StarlingApiException.class)
    public ResponseEntity<ErrorResponse> handleStarlingApiException(StarlingApiException ex) {
        HttpStatusCode status = ex.getStatus();
        String errorMessage = "An unexpected error occurred.";

        log.error("An error occurred when calling Starling API: Status: {}, Error: {}", status, ex.getMessage());

        if (status.is4xxClientError()) {
            StarlingErrorResponse starlingError = parseStarlingErrorResponse(ex.getResponseBody());
            errorMessage = starlingError.getConcatenatedErrorMessages();
        } else if (status.is5xxServerError()) {
            errorMessage = "Starling API returned an internal error. Please try again later.";
        }

        return ResponseEntity.status(status).body(new ErrorResponse(errorMessage, valueOf(status.value())));
    }

    private StarlingErrorResponse parseStarlingErrorResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, StarlingErrorResponse.class);
        } catch (Exception e) {
            log.error("Error parsing Starling API's error response: {}", e.getMessage(), e);
            StarlingErrorResponse starlingErrorResponse = new StarlingErrorResponse();
            StarlingError starlingError = new StarlingError();
            starlingError.setMessage("Unknown error from Starling API.");
            starlingErrorResponse.setErrors(List.of(starlingError));
            starlingErrorResponse.setSuccess(false);
            return starlingErrorResponse;
        }
    }
}
