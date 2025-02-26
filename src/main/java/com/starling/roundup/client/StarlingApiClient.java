package com.starling.roundup.client;

import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.common.*;
import com.starling.roundup.model.request.StarlingTransferSavingsGoalRequest;
import com.starling.roundup.model.response.StarlingBalanceResponse;
import com.starling.roundup.model.response.StarlingFeedResponse;
import com.starling.roundup.model.response.StarlingTransferSavingsGoalResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static com.starling.roundup.util.Constants.*;
import static com.starling.roundup.util.DateUtil.toStarlingDateFormat;
import static com.starling.roundup.util.DateUtil.toStarlingEndDate;
import static com.starling.roundup.util.IdUtils.generateUUID;
import static com.starling.roundup.util.LoggingUtils.maskAccountId;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

@Slf4j
@Component
public class StarlingApiClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${starling.api.token}")
    private String accessToken;

    /**
     * Retry logic using Rellience4j, not using fallback method to return a value as can not return old values
     * as data consistency is very important. Also as mentioned in the config, retries do not occur for client
     * side 4xx errors. Only server side errors.
     * @param accountUid
     * @param weekCommencing
     * @return
     */
    @Retry(name = "starlingApiRetry")
    public StarlingFeedResponse fetchTransactions(String accountUid, String maskedAccountUid, LocalDate weekCommencing) {
        log.info("Fetching settled transactions for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing);
        try {
            String startDate = toStarlingDateFormat(weekCommencing);
            String endDate = toStarlingEndDate(weekCommencing);

            ResponseEntity<StarlingFeedResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_SETTLED_TRANSACTIONS,
                    GET, getHttpEntity(), StarlingFeedResponse.class,
                    accountUid, startDate, endDate);
            if (response.getBody() == null) {
                log.error("Starling API returned null for transaction feed.");
                throw new StarlingApiException("Starling API returned null for transaction feed.");
            }
            log.info("Starling API successfully fetched {} transactions for accountUid: {}", response.getBody().getFeedItems().size(), maskedAccountUid);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Error fetching transactions for accountUid: {} - Status: {}, Response: {}", maskedAccountUid, e.getStatusCode(), e.getResponseBodyAsString());
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error fetching transactions for accountUid: {} - Exception: {}", maskedAccountUid, e.getMessage());
            throw new StarlingApiException("An error occurred when calling Starling API: " + e.getMessage());
        }
    }

    @Retry(name = "starlingApiRetry")
    public void transferToSavingsGoal(String accountUid, String goalUid, long totalRoundUpAmount) {
        String maskedAccountId = maskAccountId(accountUid);
        String maskedGoalId = maskAccountId(goalUid);
        log.info("Transferring {} minor units to savings goal {} for accountUid: {}", totalRoundUpAmount, maskedGoalId, maskedAccountId);
        try {
            StarlingTransferSavingsGoalRequest request = new StarlingTransferSavingsGoalRequest(
                    new Amount(GBP, totalRoundUpAmount));
            ResponseEntity<StarlingTransferSavingsGoalResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_SAVINGS_GOAL_TRANSFER,
                    PUT, getHttpEntity(request), StarlingTransferSavingsGoalResponse.class,
                    accountUid, goalUid, generateUUID());

            if (response.getStatusCode().isError()) {
                log.error("Failed to transfer funds to savings goal for accountUid: {} - Status: {}", maskedAccountId, response.getStatusCode());
                throw new StarlingApiException("Starling API failed to transfer funds: " + response.getStatusCode());
            }
            log.info("Successfully transferred {} minor units to savings goal {} for accountUid: {}", totalRoundUpAmount, maskedGoalId, maskedAccountId);
        } catch (HttpStatusCodeException e) {
            log.error("Error transferring funds for accountUid: {} - Status: {}, Response: {}", maskedAccountId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error transferring funds for accountUid: {} - Exception: {}", maskedAccountId, e.getMessage());
            throw new StarlingApiException("An error occurred when calling Starling API: " + e.getMessage());
        }
    }

    @Retry(name = "starlingApiRetry")
    public StarlingBalanceResponse getAccountBalance(String accountUid) {
        String maskedAccountUid = maskAccountId(accountUid);
        log.info("Fetching account balance for accountUid: {}", maskedAccountUid);
        try {
            ResponseEntity<StarlingBalanceResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_ACCOUNT_BALANCE,
                    GET, getHttpEntity(), StarlingBalanceResponse.class,
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

    private HttpEntity<?> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> getHttpEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }
}
