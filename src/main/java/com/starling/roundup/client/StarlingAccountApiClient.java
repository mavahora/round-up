package com.starling.roundup.client;

import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.response.StarlingAPIAccountsResponse;
import com.starling.roundup.model.response.StarlingAccount;
import com.starling.roundup.model.response.StarlingBalanceResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static com.starling.roundup.util.HttpEntityFactory.getHttpEntity;
import static com.starling.roundup.util.Constants.*;
import static com.starling.roundup.util.LoggingUtils.maskSensitiveData;
import static org.springframework.http.HttpMethod.GET;

@Slf4j
@Component
public class StarlingAccountApiClient {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Retry logic using Rellience4j, not using fallback method to return a value as can not return old values
     * as data consistency is very important. Also as mentioned in the config, retries do not occur for client
     * side 4xx errors. Only server side errors.
     */
    @Retry(name = "starlingApiRetry")
    public StarlingBalanceResponse getAccountBalance(String authToken, String accountUid) {
        String maskedAccountUid = maskSensitiveData(accountUid);
        log.info("Fetching account balance for accountUid: {}", maskedAccountUid);
        try {
            ResponseEntity<StarlingBalanceResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_ACCOUNT_BALANCE,
                    GET, getHttpEntity(authToken), StarlingBalanceResponse.class,
                    accountUid);

            if (response.getBody() == null) {
                log.error("Starling API returned null for balance check.");
                throw new StarlingApiException("Starling API returned null for balance check.");
            }
            log.info("Fetched account balance for accountUid: {}", maskedAccountUid);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Error fetching balance for accountUid: {} - Status: {}, Response: {}", maskedAccountUid, e.getStatusCode(), e.getResponseBodyAsString());
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error fetching balance for accountUid: {} - Exception: {}", maskedAccountUid, e.getMessage());
            throw new StarlingApiException("An error occurred when calling Starling API balance check: " + e.getMessage());
        }
    }

    @Retry(name = "starlingApiRetry")
    public StarlingAccount getPrimaryAccount(String authToken) {
        String maskedToken = maskSensitiveData(authToken);
        log.info("Fetching account details using Auth Token: {}", maskedToken);
        try {
            ResponseEntity<StarlingAPIAccountsResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_ACCOUNT_DETAILS,
                    GET, getHttpEntity(authToken), StarlingAPIAccountsResponse.class);

            StarlingAPIAccountsResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getAccounts() == null || responseBody.getAccounts().isEmpty()) {
                log.error("Starling API returned null when retrieving accounts for token: {}" , maskedToken);
                throw new StarlingApiException("Starling API returned null when retrieving accounts for token: " + maskedToken);
            }
            Optional<StarlingAccount> primaryAccount = response.getBody().getAccounts().stream()
                    .filter(account -> ACCOUNT_PRIMARY.equals(account.getAccountType()))
                    .findFirst();

            return responseBody.getAccounts().stream()
                    .filter(account -> ACCOUNT_PRIMARY.equals(account.getAccountType()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("No primary account found for token: {}", maskedToken);
                        return new StarlingApiException("No primary account linked with token: " + maskedToken);
                    });
        } catch (HttpStatusCodeException e) {
            log.error("Error fetching accounts for token: {} - Status: {}, Response: {}", maskedToken, e.getStatusCode(), e.getResponseBodyAsString());
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error fetching accounts for token: {} - Exception: {}", maskedToken, e.getMessage());
            throw new StarlingApiException("An error occurred when calling Starling API balance check: " + e.getMessage());
        }
    }
}
