package com.starling.roundup.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
public class ErrorResponse {
    private String error;
    private LocalDateTime timestamp;
    @JsonInclude(NON_NULL)
    private String starlingApiResponseStatus;

    public ErrorResponse(String error) {
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String error, String starlingApiResponseStatus) {
        this.error = error;
        this.timestamp = LocalDateTime.now();
        this.starlingApiResponseStatus = starlingApiResponseStatus;
    }
}