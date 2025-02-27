package com.starling.roundup.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

public class HttpEntityFactory {

    public static HttpEntity<?> getHttpEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    public static <T> HttpEntity<T> getHttpEntity(String accessToken, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }
}
