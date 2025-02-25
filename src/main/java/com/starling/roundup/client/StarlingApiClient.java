package com.starling.roundup.client;

import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.dto.*;
import io.github.resilience4j.retry.annotation.Retry;
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
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

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
    public StarlingFeedResponse fetchTransactions(String accountUid, LocalDate weekCommencing) {
        try {
            String startDate = toStarlingDateFormat(weekCommencing);
            String endDate = toStarlingEndDate(weekCommencing);

            ResponseEntity<StarlingFeedResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_SETTLED_TRANSACTIONS,
                    GET, getHttpEntity(), StarlingFeedResponse.class,
                    accountUid, startDate, endDate);
            if (response.getBody() == null) {
                throw new StarlingApiException("Starling API returned null for transaction feed.");
            }
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new StarlingApiException("An error occurred when calling Starling API: " + e.getMessage());
        }
    }

    @Retry(name = "starlingApiRetry")
    public void transferToSavingsGoal(String accountUid, String goalUid, long totalRoundUpAmount) {
        try {
            StarlingTransferSavingsGoalRequest request = new StarlingTransferSavingsGoalRequest(
                    new Amount(GBP, totalRoundUpAmount));
            ResponseEntity<StarlingTransferSavingsGoalResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_SAVINGS_GOAL_TRANSFER,
                    PUT, getHttpEntity(request), StarlingTransferSavingsGoalResponse.class,
                    accountUid, goalUid, generateUUID());

            if (response.getStatusCode().isError()) {
                throw new StarlingApiException("Starling API failed to transfer funds: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new StarlingApiException("An error occurred when calling Starling API: " + e.getMessage());
        }
    }

    @Retry(name = "starlingApiRetry")
    public StarlingBalanceResponse getAccountBalance(String accountUid) {
        try {
            ResponseEntity<StarlingBalanceResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_ACCOUNT_BALANCE,
                    GET, getHttpEntity(), StarlingBalanceResponse.class,
                    accountUid);

            if (response.getBody() == null) {
                throw new StarlingApiException("Starling API returned null for balance check.");
            }

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
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
