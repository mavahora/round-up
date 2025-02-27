package com.starling.roundup.controller;

import com.starling.roundup.model.request.RoundUpRequest;
import com.starling.roundup.model.response.AccountDetailsResponse;
import com.starling.roundup.model.response.RoundUpStatusResponse;
import com.starling.roundup.service.AccountDetailsService;
import com.starling.roundup.service.RoundUpService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.starling.roundup.util.LoggingUtils.maskSensitiveData;
import static com.starling.roundup.util.TokenUtils.extractToken;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Slf4j
@RestController
@RequestMapping("/api")
public class RoundUpController {

    @Autowired
    private AccountDetailsService accountDetailsService;

    @Autowired
    private RoundUpService roundUpService;

    @GetMapping("/account/saving-goals")
    public ResponseEntity<AccountDetailsResponse> getAccountDetails(@RequestHeader("Authorization") String bearerToken) {
        AccountDetailsResponse response = accountDetailsService.getAccountDetails(extractToken(bearerToken));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/round-up")
    public ResponseEntity<RoundUpStatusResponse> roundUp(@Valid @RequestBody RoundUpRequest request) {
        String maskedAccountId = maskSensitiveData(request.getAccountUid());
        log.info("Received request to calculate roundup for accountId: {}", maskedAccountId);
        ResponseEntity<RoundUpStatusResponse> response =  roundUpService.initiateRoundUp(
                request.getAccountUid(),
                maskedAccountId,
                request.getSavingsGoalUid(),
                request.isWeekCommencingValid());
        log.info("Calculated roundup amount for accountId: {}", maskedAccountId);
        return response;

    }

    @GetMapping("/round-up/status/{accountId}/{weekCommencing}")
    public ResponseEntity<RoundUpStatusResponse> checkRoundUpStatus(
            @PathVariable String accountId, @PathVariable @DateTimeFormat(iso = DATE) LocalDate weekCommencing) {
        String maskedAccountId = maskSensitiveData(accountId);
        log.info("Received request to get status of roundup for accountId: {} and weekCommencing: {}", maskedAccountId, weekCommencing);
        ResponseEntity<RoundUpStatusResponse> response =  roundUpService.checkRoundUpStatus(accountId, maskedAccountId, weekCommencing);
        log.info("Returning response with status of roundup for accountId: {} and weekCommencing: {}", maskedAccountId, weekCommencing);
        return response;
    }

}
