package com.starling.roundup.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(StarlingApiException.class)
    public ResponseEntity<Map<String, Object>> handleStarlingApiException(StarlingApiException ex) {
        HttpStatusCode status = ex.getStatus();
        String errorMessage = "An unexpected error occurred.";

        if (status.is4xxClientError()) {
            errorMessage = parseStarlingErrorResponse(ex.getResponseBody());
        } else if (status.is5xxServerError()) {
            errorMessage = "Starling API encountered an internal error. Please try again later.";
        } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            errorMessage = "Starling API is currently unavailable. Please try again later.";
        }

        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", errorMessage,
                        "status", status.value(),
                        "timestamp", LocalDateTime.now()
                ));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFundsException(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage(), "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong", "details", ex.getMessage(), "timestamp", LocalDateTime.now()));
    }

    private String parseStarlingErrorResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "Unknown error from Starling API.";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("errors").get(0).path("message").asText("Unknown error.");
        } catch (Exception e) {
            return "Invalid error response format from Starling API.";
        }
    }
}
