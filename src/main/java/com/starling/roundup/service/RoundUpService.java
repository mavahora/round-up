package com.starling.roundup.service;

import com.starling.roundup.entity.RoundUpRequest;
import com.starling.roundup.model.response.RoundUpStatusResponse;
import com.starling.roundup.repository.RoundUpRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static com.starling.roundup.entity.RoundUpStatus.*;
import static com.starling.roundup.model.response.RoundUpStatusResponse.ALREADY_IN_PROGRESS;
import static com.starling.roundup.util.IdUtils.generateUUID;
import static com.starling.roundup.util.LoggingUtils.maskAccountId;
import static org.springframework.http.HttpStatus.CONFLICT;

@Slf4j
@Service
public class RoundUpService {

    private final RoundUpRequestRepository roundUpRequestRepository;
    private final RoundUpAsyncService roundUpAsyncService;
    private final RedissonClient redissonClient;

    public RoundUpService(RoundUpRequestRepository roundUpRequestRepository, RoundUpAsyncService roundUpAsyncService, RedissonClient redissonClient) {
        this.roundUpRequestRepository = roundUpRequestRepository;
        this.roundUpAsyncService = roundUpAsyncService;
        this.redissonClient = redissonClient;
    }

    /**
     * This method initiates the round-up request, I have ensured idempotency is achieved and ensured that
     * each round-up is only processed ONCE for each account and week. This is achieved by checking the database
     * first to see if there is an entry with the particular week and account, if there is then check if it is
     * completed or in progress and return the status of this request. If the request is in a FAILED status
     * or is a new request then proceed with locking this request so that it can be processed.
     * In the real world, Redis would be deployed on the cloud and shared across multiple instances of
     * this microservice (making use of distributed locking). This allows scalability by increasing
     * instances of this microservice without affecting data consistency.
     * The method tries to get a Redis lock for this account and week (which is unique), this prevents multiple
     * processes from running the same roundup at once. This could happen when a user is maybe logged in
     * to 10 devices and clicks round-up at the same time in all 10 devices for the same week
     * and account. In this case the first process would get the lock and start the processing,
     * the other 9 requests will fall into the else statement without blocking where a response saying ALREADY_IN_PROGRESS
     * will be returned. This can then be handled on the client side where it will have some logic to do
     * certain things depending on the response.
     * For instance, if the client is returned with an IN_PROGRESS or ALREADY_IN_PROGRESS status, it could display
     * a message to the user that the round-up is being processed. The client would then use smart polling
     * to check the status every second for say 5 seconds, then check every 5 seconds up to 30 seconds to prevent
     * overloading the server.
     *
     * @param accountUid
     * @param maskedAccountUid
     * @param goalUid
     * @param weekCommencing
     * @return
     */
    public ResponseEntity<Map<String, Object>> initiateRoundUp(String accountUid, String maskedAccountUid, String goalUid, LocalDate weekCommencing) {
        log.info("Initiating round-up for accountUid: {}, goalUid: {}, weekCommencing: {}", maskedAccountUid, maskAccountId(goalUid), weekCommencing);
        Optional<RoundUpRequest> existingRequest = roundUpRequestRepository.findByAccountIdAndWeekCommencing(accountUid, weekCommencing);

        if (existingRequest.isPresent()) {
            RoundUpRequest request = existingRequest.get();
            log.info("Found existing request with status: {}", request.getStatus());

            // If request is already completed, return COMPLETED status with round-up amount
            if (request.getStatus() == COMPLETED) {
                log.info("Round-up already completed for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing);
                return ResponseEntity.ok(Map.of(
                        "status", RoundUpStatusResponse.ALREADY_COMPLETED,
                        "roundUpAmount", request.getRoundUpAmount()
                ));
            }

            // If request is in progress, return IN_PROGRESS response to prevent duplicate procesing
            if (request.getStatus() == IN_PROGRESS) {
                log.warn("Round-up already in progress for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing);
                return ResponseEntity.status(CONFLICT).body(Map.of("status", ALREADY_IN_PROGRESS));
            }
        }
        // Unique lock with accountid & week commencing to prevent race conditions where multiple
        // requests may try rounding up simultaneously.
        RLock lock = redissonClient.getLock("roundup-lock:" + accountUid + ":" + weekCommencing);
        if (lock.tryLock()) {
            log.debug("Acquired lock to process round-up for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing);
            try {
                RoundUpRequest request;
                // If the status is FAILED, it means there was an issue previously. You can retry for the same
                // account and week by updating the status to IN_PROGRESS. In the future a retry count could
                // be implemented to limit re-tries
                if (existingRequest.isPresent() && existingRequest.get().getStatus() == FAILED) {
                    request = existingRequest.get();
                    request.setStatus(IN_PROGRESS);
                    log.info("Retrying failed round-up for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing);
                } else {
                    // Create a new round-up request for this week and account
                    request = createNewRoundUpRequest(accountUid, weekCommencing);
                    log.info("Created new round-up request: {} for accountUid: {}, weekCommencing: {}",request.getRequestId(), maskedAccountUid, weekCommencing);
                }
                roundUpRequestRepository.save(request);
                log.debug("Round-up request: {} saved in DB", request.getRequestId());

                // Process calling the Starling APIs, calculation and updating of database asynchronously
                roundUpAsyncService.processRoundUpAsync(request.getRequestId(), accountUid, maskedAccountUid, goalUid, weekCommencing);
                log.info("Round-up processing started asynchronously for requestId: {}", request.getRequestId());

                // Return a 202 accepted response with the requestID so the client can poll and check the porgress
                // of the round up.
                return ResponseEntity.accepted().body(Map.of("status", RoundUpStatusResponse.IN_PROGRESS, "requestId", request.getRequestId()));
            } catch (Exception e) {
                log.error("Error occurred when initiating round-up for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing, e);
                throw e;
            }finally {
                lock.unlock();
            }
        } else {
            log.debug("Could not acquire lock as round-up is already in progress for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing);
            return ResponseEntity.ok(Map.of("status", ALREADY_IN_PROGRESS));
        }
    }

    /**
     *
     * @param accountId
     * @param maskedAccountId
     * @param weekCommencing
     * @return
     */
    public ResponseEntity<Map<String, Object>> checkRoundUpStatus(String accountId, String maskedAccountId, LocalDate weekCommencing) {
        log.info("Checking round-up status for accountId: {}, weekCommencing: {}", maskedAccountId, weekCommencing);
        Optional<RoundUpRequest> requestOpt = roundUpRequestRepository.findByAccountIdAndWeekCommencing(accountId, weekCommencing);

        if (requestOpt.isEmpty()) {
            log.warn("No round-up request found for accountId: {}, weekCommencing: {}", maskedAccountId, weekCommencing);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "NOT_FOUND"));
        }

        RoundUpRequest request = requestOpt.get();
        log.info("Round-up request found with status: {} for accountId: {}, weekCommencing: {}", request.getStatus(), maskedAccountId, weekCommencing);
        return ResponseEntity.ok(Map.of(
                "status", request.getStatus(),
                "roundUpAmount", request.getStatus() == COMPLETED ? request.getRoundUpAmount() : null
        ));
    }

    private RoundUpRequest createNewRoundUpRequest(String accountUid, LocalDate weekCommencing) {
        RoundUpRequest request = new RoundUpRequest();
        request.setRequestId(generateUUID());
        request.setAccountId(accountUid);
        request.setWeekCommencing(weekCommencing);
        request.setStatus(IN_PROGRESS);
        return request;
    }
}