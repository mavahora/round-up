package com.starling.roundup.service;

import com.starling.roundup.client.StarlingAccountApiClient;
import com.starling.roundup.client.StarlingGoalsApiClient;
import com.starling.roundup.model.response.AccountDetailsResponse;
import com.starling.roundup.model.response.SavingsGoal;
import com.starling.roundup.model.response.StarlingAccount;
import com.starling.roundup.model.wrapper.SavingsGoalsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.starling.roundup.util.LoggingUtils.maskSensitiveData;

@Slf4j
@Service
public class AccountDetailsService {

    private final StarlingAccountApiClient accountsApiClient;
    private final StarlingGoalsApiClient goalsApiClient;

    public AccountDetailsService(StarlingAccountApiClient accountsApiClient, StarlingGoalsApiClient goalsApiClient) {
        this.accountsApiClient = accountsApiClient;
        this.goalsApiClient = goalsApiClient;
    }

    public AccountDetailsResponse getAccountDetails(String authToken) {
        // Fetch user's primary account using token
        StarlingAccount account = fetchPrimaryAccount(authToken);

        // Fetch all active GBP savings goals, or create a new one if none available
        SavingsGoalsWrapper savingGoals = fetchOrCreateActiveGoals(authToken, account.getAccountUid());

        // Build response with Account and active Savings Goal
        return buildAccountDetailsResponse(account, savingGoals);
    }

    private StarlingAccount fetchPrimaryAccount(String authToken) {
        String maskedToken = maskSensitiveData(authToken);
        // Fetch accounts and return primary  account
        StarlingAccount account = accountsApiClient.getPrimaryAccount(authToken);
        log.info("Successfully retrieved Primary account for token: {}", maskedToken);
        return account;
    }

    private SavingsGoalsWrapper fetchOrCreateActiveGoals(String authToken, String accountUid) {
        // Fetch all active GBP savings goals
        List<SavingsGoal> savingsGoals = goalsApiClient.getActiveGoals(authToken, accountUid);
        boolean createdNewGoal = false;
        if (savingsGoals.isEmpty()) {
            // Create a new goal if none exists
            savingsGoals = goalsApiClient.createNewGoal(authToken, accountUid);
            createdNewGoal = true;
        }

        return new SavingsGoalsWrapper(savingsGoals, createdNewGoal);
    }

    private AccountDetailsResponse buildAccountDetailsResponse(StarlingAccount account, SavingsGoalsWrapper savingsGoals) {
        AccountDetailsResponse response = new AccountDetailsResponse();
        response.setAccountUid(account.getAccountUid());
        response.setAccountName(account.getName());
        response.setSavingsGoalList(savingsGoals.getSavingsGoals());
        if (savingsGoals.isNewGoalCreated()) response.setMessage("No valid saving goals found, new savings goal created");

        return response;
    }

}