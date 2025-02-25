package com.starling.roundup.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class StarlingApiException extends RuntimeException {
    private HttpStatusCode status;
    private String responseBody;

    public StarlingApiException(String message) {
        super(message);
    }

    public StarlingApiException(HttpStatusCode status, String responseBody) {
        super(responseBody);  // Store response body as message
        this.status = status;
        this.responseBody = responseBody;
    }

}