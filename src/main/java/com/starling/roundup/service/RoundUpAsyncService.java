package com.starling.roundup.service;

import com.starling.roundup.client.StarlingApiClient;
import com.starling.roundup.entity.RoundUpStatus;
import com.starling.roundup.exception.InsufficientFundsException;
import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.dto.StarlingFeedResponse;
import com.starling.roundup.repository.RoundUpRequestRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.starling.roundup.entity.RoundUpStatus.COMPLETED;
import static com.starling.roundup.entity.RoundUpStatus.FAILED;
import static com.starling.roundup.util.CurrencyConverter.convertToGBP;

@Service
public class RoundUpAsyncService {

    private final StarlingApiClient starlingApiClient;
    private final RoundUpRequestRepository roundUpRequestRepository;

    public RoundUpAsyncService(StarlingApiClient starlingApiClient, RoundUpRequestRepository roundUpRequestRepository) {
        this.starlingApiClient = starlingApiClient;
        this.roundUpRequestRepository = roundUpRequestRepository;
    }

    @Async
    public void processRoundUpAsync(String accountUid, String goalUid, LocalDate weekCommencing) {
        try {
            StarlingFeedResponse response = starlingApiClient.fetchTransactions(accountUid, weekCommencing);

            long totalRoundUpAmount = calculateRoundUpAmount(response);

            if (totalRoundUpAmount == 0) {
                updateRoundUpStatus(accountUid, weekCommencing, FAILED, 0);
                throw new StarlingApiException("No transactions eligible for round-up.");
            }

            if (!hasSufficientFunds(accountUid, totalRoundUpAmount)) {
                updateRoundUpStatus(accountUid, weekCommencing, FAILED, 0);
                throw new InsufficientFundsException("Not enough funds available for transfer.");
            }

            starlingApiClient.transferToSavingsGoal(accountUid, goalUid, totalRoundUpAmount);
            updateRoundUpStatus(accountUid, weekCommencing, COMPLETED, totalRoundUpAmount);
        } catch (Exception e) {
            // No need to throw exception as this is a void Async method, client would have already
            // received a 202 response. Update status as FAILED.
            updateRoundUpStatus(accountUid, weekCommencing, FAILED, 0);
        }

    }

    private static long calculateRoundUpAmount(StarlingFeedResponse response) {
        return response.getFeedItems().stream()
                // If currency is not GBP, convert to GBP and return minorUnits so we can round-up change in GBP
                .map(feedItem -> convertToGBP(feedItem.getAmount())) // Amount in minor units/pence (GBP)
                .filter(amount -> amount % 100 != 0)  // If the amount is a whole pound, you can not round up
                .mapToLong(amount -> 100 - (amount % 100))  // Calculate round-up amount for each transaction
                .sum();
    }

    public boolean hasSufficientFunds(String accountUid, long amountToTransfer) {
        BigDecimal effectiveBalance = starlingApiClient.getAccountBalance(accountUid).getEffectiveBalance().getAmountInPounds();
        return effectiveBalance.compareTo(BigDecimal.valueOf(amountToTransfer).divide(BigDecimal.valueOf(100))) >= 0;
    }

    private void updateRoundUpStatus(String accountUid, LocalDate weekCommencing, RoundUpStatus status, long amount) {
        roundUpRequestRepository.updateStatusAndAmountByAccountAndWeek(accountUid, weekCommencing, status, amount);
    }
}