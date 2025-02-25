package com.starling.roundup.util;

public class Constants {

    public static final String API_BASE_URL = "https://api-sandbox.starlingbank.com";
    public static final String API_SETTLED_TRANSACTIONS = "/api/v2/feed/account/{accountUid}/settled-transactions-between?minTransactionTimestamp={minTransactionTimestamp}&maxTransactionTimestamp={maxTransactionTimestamp}";
    public static final String API_ACCOUNT_BALANCE = "/api/v2/accounts/{accountUid}/balance";
    public static final String API_SAVINGS_GOAL_TRANSFER = "/api/v2/account/{accountUid}/savings-goals/{savingsGoalUid}/add-money/{transferUid}";


    public static final String GBP = "GBP";

}
