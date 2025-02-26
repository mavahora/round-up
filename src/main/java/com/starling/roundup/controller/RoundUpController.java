package com.starling.roundup.controller;

import com.starling.roundup.service.RoundUpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.starling.roundup.util.LoggingUtils.maskAccountId;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Slf4j
@RestController
@RequestMapping("/api")
public class RoundUpController {

    @Autowired
    private RoundUpService roundUpService;

    @PostMapping("/round-up/{accountId}/{goalUid}")
    public ResponseEntity<Map<String, Object>> roundUp(@PathVariable String accountId, @PathVariable String goalUid,
                                          @RequestParam @DateTimeFormat(iso = DATE) LocalDate weekCommencing) {
        String maskedAccountId = maskAccountId(accountId);
        log.info("Received request to calculate roundup for accountId: {}", maskedAccountId);
        ResponseEntity<Map<String, Object>> response =  roundUpService.initiateRoundUp(accountId, maskedAccountId, goalUid, weekCommencing);
        log.info("Calculated roundup amount for accountId: {}", maskedAccountId);
        return response;

    }

    @GetMapping("/round-up/status/{accountId}/{weekCommencing}")
    public ResponseEntity<Map<String, Object>> checkRoundUpStatus(
            @PathVariable String accountId, @PathVariable @DateTimeFormat(iso = DATE) LocalDate weekCommencing) {
        String maskedAccountId = maskAccountId(accountId);
        log.info("Received request to get status of roundup for accountId: {} and weekCommencing: {}", maskedAccountId, weekCommencing);
        ResponseEntity<Map<String, Object>> response =  roundUpService.checkRoundUpStatus(accountId, maskedAccountId, weekCommencing);
        log.info("Returning response with status of roundup for accountId: {} and weekCommencing: {}", maskedAccountId, weekCommencing);
        return response;
    }

}
