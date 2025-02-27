package com.starling.roundup.client;

import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.common.Amount;
import com.starling.roundup.model.request.SavingsGoalRequest;
import com.starling.roundup.model.request.StarlingTransferSavingsGoalRequest;
import com.starling.roundup.model.response.CreateSavingsGoalResponse;
import com.starling.roundup.model.response.SavingsGoal;
import com.starling.roundup.model.response.StarlingAPISavingGoalsResponse;
import com.starling.roundup.model.response.StarlingTransferSavingsGoalResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.starling.roundup.util.Constants.*;
import static com.starling.roundup.util.DateUtil.getCurrentDateTime;
import static com.starling.roundup.util.HttpEntityFactory.getHttpEntity;
import static com.starling.roundup.util.IdUtils.generateUUID;
import static com.starling.roundup.util.LoggingUtils.maskSensitiveData;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

@Slf4j
@Component
public class StarlingGoalsApiClient {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Retry logic using Rellience4j, not using fallback method to return a value as can not return old values
     * as data consistency is very important. Also as mentioned in the config, retries do not occur for client
     * side 4xx errors. Only server side errors.
     */
    @Retry(name = "starlingApiRetry")
    public void transferToSavingsGoal(String authToken, String accountUid, String goalUid, long totalRoundUpAmount) {
        String maskedAccountId = maskSensitiveData(accountUid);
        String maskedGoalId = maskSensitiveData(goalUid);
        log.info("Transferring {} minor units to savings goal {} for accountUid: {}", totalRoundUpAmount, maskedGoalId, maskedAccountId);
        try {
            StarlingTransferSavingsGoalRequest request = new StarlingTransferSavingsGoalRequest(
                    new Amount(GBP, totalRoundUpAmount));
            ResponseEntity<StarlingTransferSavingsGoalResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_SAVINGS_GOAL_TRANSFER,
                    PUT, getHttpEntity(authToken, request), StarlingTransferSavingsGoalResponse.class,
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

    public List<SavingsGoal> getActiveGoals(String authToken, String accountUid) {
        String maskedAccountUid = maskSensitiveData(accountUid);
        log.info("Fetching savings goal details for account: {}", maskedAccountUid);
        try {
            ResponseEntity<StarlingAPISavingGoalsResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_FETCH_ALL_SAVINGS_GOAL,
                    GET, getHttpEntity(authToken), StarlingAPISavingGoalsResponse.class, accountUid);

            StarlingAPISavingGoalsResponse responseBody = response.getBody();
            if (responseBody == null || CollectionUtils.isEmpty((responseBody.getSavingsGoalList()))) {
                log.info("No savings goals linked to account: {}", maskedAccountUid);
                return emptyList();
            }

            List<SavingsGoal> savingsGoals =  responseBody.getSavingsGoalList().stream()
                .filter(goal -> SAVINGS_GOAL_ACTIVE.equals(goal.getState()))
                .filter(goal -> GBP.equals(goal.getTarget().getCurrency()))
                .collect(Collectors.toList());

            if (savingsGoals.isEmpty()) {
                log.error("No ACTIVE GBP savings goals linked to account: {}", maskedAccountUid);
                return emptyList();
            };
            log.info("Starling API Successfully returned {} active saving goals for accountUid: {}", savingsGoals.size(), maskedAccountUid);
            return savingsGoals;
        } catch (HttpStatusCodeException e) {
            log.error("Error fetching saving goals for accountUid: {} - Status: {}, Response: {}", accountUid, e.getStatusCode(), e.getResponseBodyAsString());
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error fetching saving goals for accountUid: {} - Exception: {}", accountUid, e.getMessage());
            throw new StarlingApiException("An error occurred when calling Starling API to retrieve savings goals: " + e.getMessage());
        }
    }

    public List<SavingsGoal> createNewGoal(String authToken, String accountUid) {
        String maskedAccountUid = maskSensitiveData(accountUid);
        log.info("Creating new savings goal for account: {}", maskedAccountUid);
        try {
            SavingsGoalRequest request = buildSavingsGoalRequest();
            ResponseEntity<CreateSavingsGoalResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_CREATE_SAVINGS_GOAL,
                    PUT, getHttpEntity(authToken, request), CreateSavingsGoalResponse.class, accountUid);

            if (response.getBody() == null || !response.getBody().isSuccess()) {
                log.error("Error creating new savings goal for accountUid: {}", maskedAccountUid);
                throw new StarlingApiException("Error creating new savings goal for accountUid:" + accountUid);
            }

            log.info("Successfully created new savings goal for accountUid: {} with name: {}", maskedAccountUid, request.getName());

            return singletonList(new SavingsGoal(response.getBody().getSavingsGoalUid(), request.getName()));
        } catch (HttpStatusCodeException e) {
            log.error("Error creating new savings goal for accountUid: {} - Status: {}, Response: {}", maskedAccountUid, e.getStatusCode(), e.getResponseBodyAsString());
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error creating new savings goal for accountUid: {} - Exception: {}", maskedAccountUid, e.getMessage());
            throw new StarlingApiException("An error occurred when calling Starling API to create a new savings goal: " + e.getMessage());
        }
    }

    private static SavingsGoalRequest buildSavingsGoalRequest() {
        SavingsGoalRequest request = new SavingsGoalRequest();
        request.setName("SavingsGoal_"+ getCurrentDateTime());
        request.setCurrency(GBP);
        request.setTarget(new Amount(GBP, 10000));
        request.setBase64EncodedPhoto("string");
        return request;
    }
}
