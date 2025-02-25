package com.starling.roundup.controller;

import com.starling.roundup.entity.RoundUpRequest;
import com.starling.roundup.service.RoundUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api")
public class RoundUpController {

    @Autowired
    private RoundUpService roundUpService;

    @PostMapping("/round-up/{accountId}/{goalUid}")
    public ResponseEntity<Map<String, Object>> roundUp(@PathVariable String accountId, @PathVariable String goalUid,
                                          @RequestParam @DateTimeFormat(iso = DATE) LocalDate weekCommencing) {

        return roundUpService.initiateRoundUp(accountId, goalUid, weekCommencing);
    }

    @GetMapping("/round-up/status/{accountId}/{weekCommencing}")
    public ResponseEntity<Map<String, Object>> checkRoundUpStatus(
            @PathVariable String accountId, @PathVariable @DateTimeFormat(iso = DATE) LocalDate weekCommencing) {

        return roundUpService.checkRoundUpStatus(accountId, weekCommencing);
    }

}
