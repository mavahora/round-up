package com.starling.roundup.service;

import com.starling.roundup.client.StarlingApiClient;
import com.starling.roundup.entity.RoundUpStatus;
import com.starling.roundup.exception.InsufficientFundsException;
import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.dto.StarlingFeedResponse;
import com.starling.roundup.repository.RoundUpRequestRepository;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.starling.roundup.entity.RoundUpStatus.COMPLETED;
import static com.starling.roundup.entity.RoundUpStatus.FAILED;
import static com.starling.roundup.util.Constants.GBP;

@Service
public class RoundUpAsyncService {

    private final StarlingApiClient starlingApiClient;
    private final RoundUpRequestRepository roundUpRequestRepository;
    private final RedissonClient redissonClient;

    public RoundUpAsyncService(StarlingApiClient starlingApiClient, RoundUpRequestRepository roundUpRequestRepository, RedissonClient redissonClient) {
        this.starlingApiClient = starlingApiClient;
        this.roundUpRequestRepository = roundUpRequestRepository;
        this.redissonClient = redissonClient;
    }

    @Async
    public void processRoundUpAsync(String accountUid, String goalUid, LocalDate weekCommencing) {
        try {
            StarlingFeedResponse response = starlingApiClient.fetchTransactions(accountUid, weekCommencing);

            long totalRoundUpAmount = response.getFeedItems().stream()
                    .filter(feedItem -> GBP.equals(feedItem.getAmount().getCurrency()))  // Filter on GBP transactions
                    .map(feedItem -> feedItem.getAmount().getMinorUnits())  // Get the amount in pence
                    .filter(amount -> amount % 100 != 0)  // If the amount is a whole pound, you can not round up
                    .mapToLong(amount -> 100 - (amount % 100))  // Calculate round-up amount for each transaction
                    .sum();

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
            updateRoundUpStatus(accountUid, weekCommencing, FAILED, 0);
            throw e;
        }

    }

    public boolean hasSufficientFunds(String accountUid, long amountToTransfer) {
        BigDecimal effectiveBalance = starlingApiClient.getAccountBalance(accountUid).getEffectiveBalance().getAmountInPounds();
        return effectiveBalance.compareTo(BigDecimal.valueOf(amountToTransfer).divide(BigDecimal.valueOf(100))) >= 0;
    }

    private void updateRoundUpStatus(String accountUid, LocalDate weekCommencing, RoundUpStatus status, long amount) {
        roundUpRequestRepository.updateStatusAndAmountByAccountAndWeek(accountUid, weekCommencing, status, amount);
    }
}