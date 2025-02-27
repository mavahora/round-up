package com.starling.roundup.service;

import com.starling.roundup.client.StarlingAccountApiClient;
import com.starling.roundup.client.StarlingGoalsApiClient;
import com.starling.roundup.client.StarlingTransactionApiClient;
import com.starling.roundup.entity.Status;
import com.starling.roundup.exception.InsufficientFundsException;
import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.response.StarlingFeedResponse;
import com.starling.roundup.repository.RoundUpRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.starling.roundup.entity.Status.COMPLETED;
import static com.starling.roundup.entity.Status.FAILED;
import static com.starling.roundup.model.response.TransactionDirection.OUT;
import static com.starling.roundup.util.Constants.GBP;
import static com.starling.roundup.util.CurrencyConverter.convertToGBP;
import static com.starling.roundup.util.LoggingUtils.maskSensitiveData;

@Slf4j
@Service
public class RoundUpAsyncService {

    private final StarlingAccountApiClient accountsApiClient;
    private final StarlingGoalsApiClient goalsApiClient;
    private final StarlingTransactionApiClient transactionApiClient;
    private final RoundUpRequestRepository roundUpRequestRepository;

    public RoundUpAsyncService(StarlingAccountApiClient accountsApiClient, StarlingGoalsApiClient goalsApiClient, StarlingTransactionApiClient transactionApiClient, RoundUpRequestRepository roundUpRequestRepository) {
        this.accountsApiClient = accountsApiClient;
        this.goalsApiClient = goalsApiClient;
        this.transactionApiClient = transactionApiClient;
        this.roundUpRequestRepository = roundUpRequestRepository;
    }

    @Async
    public void processRoundUpAsync(String authToken, String requestId, String accountUid, String maskedAccountUid, String goalUid, LocalDate weekCommencing) {
        log.info("RequestId: {}, Starting round-up processing asynchronously for accountUid: {}, weekCommencing: {}", requestId, maskedAccountUid, weekCommencing);
        try {
            StarlingFeedResponse response = transactionApiClient.fetchTransactions(authToken, accountUid, maskedAccountUid, weekCommencing);
            log.info("RequestId: {}, Response received from Starling Settled Transactions API for round-up calculation.", requestId);

            long totalRoundUpAmount = calculateRoundUpAmount(requestId, response);
            log.debug("RequestId: {}, Calculated total round-up amount: {} minor units.", requestId, totalRoundUpAmount);

            if (totalRoundUpAmount == 0) {
                log.warn("RequestId: {}, No transactions are eligible for round-up.", requestId);
                updateRoundUpStatus(accountUid, weekCommencing, FAILED, 0);
                throw new StarlingApiException("No transactions eligible for round-up.");
            }

            if (!hasSufficientFunds(authToken, accountUid, totalRoundUpAmount)) {
                log.warn("RequestId: {}, Insufficient funds for round-up transfer.", requestId);
                updateRoundUpStatus(accountUid, weekCommencing, FAILED, 0);
                throw new InsufficientFundsException("Not enough funds available for transfer.");
            }

            goalsApiClient.transferToSavingsGoal(authToken, accountUid, goalUid, totalRoundUpAmount);
            updateRoundUpStatus(accountUid, weekCommencing, COMPLETED, totalRoundUpAmount);
            log.info("RequestId: {}, Successfully completed round-up transfer of {} minor units.", requestId, totalRoundUpAmount);
        } catch (Exception e) {
            // No need to throw exception as this is a void Async method, client would have already
            // received a 202 response. Update status as FAILED.
            log.error("RequestId: {}, Error processing round-up: {}", requestId, e.getMessage());
            updateRoundUpStatus(accountUid, weekCommencing, FAILED, 0);
        }

    }

    private static long calculateRoundUpAmount(String requestId, StarlingFeedResponse response) {
        return response.getFeedItems().stream()
                .filter(feedItem -> OUT.equals(feedItem.getDirection())) // Only look at transactions going out
                .map(feedItem -> {
                    if (GBP.equals(feedItem.getAmount().getCurrency())) {
                        // If currency is GBP, no need to convert minor units
                        return feedItem.getAmount().getMinorUnits();
                    }
                    // Convert to GBP and return minor units
                    return convertToGBP(requestId, feedItem.getAmount());
                })
                .filter(amount -> amount % 100 != 0)  // If the amount is a whole pound, you can not round up
                .mapToLong(amount -> 100 - (amount % 100))  // Calculate round-up amount for each transaction
                .sum();
    }

    public boolean hasSufficientFunds(String authToken, String accountUid, long amountToTransfer) {
        BigDecimal effectiveBalance = accountsApiClient.getAccountBalance(authToken, accountUid).getEffectiveBalance().getAmountInPounds();
        return effectiveBalance.compareTo(BigDecimal.valueOf(amountToTransfer).divide(BigDecimal.valueOf(100))) >= 0;
    }

    private void updateRoundUpStatus(String accountUid, LocalDate weekCommencing, Status status, long amount) {
        log.info("Updating round-up status for accountUid: {}, weekCommencing: {}, status: {}, amount: {}", maskSensitiveData(accountUid), weekCommencing, status, amount);
        roundUpRequestRepository.updateStatusAndAmountByAccountAndWeek(accountUid, weekCommencing, status, amount);
    }
}