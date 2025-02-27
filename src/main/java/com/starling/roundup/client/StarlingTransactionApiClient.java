package com.starling.roundup.client;

import com.starling.roundup.exception.StarlingApiException;
import com.starling.roundup.model.response.StarlingFeedResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static com.starling.roundup.util.HttpEntityFactory.getHttpEntity;
import static com.starling.roundup.util.Constants.API_BASE_URL;
import static com.starling.roundup.util.Constants.API_SETTLED_TRANSACTIONS;
import static com.starling.roundup.util.DateUtil.toStarlingDateFormat;
import static com.starling.roundup.util.DateUtil.toStarlingEndDate;
import static org.springframework.http.HttpMethod.GET;

@Slf4j
@Component
public class StarlingTransactionApiClient {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Retry logic using Rellience4j, not using fallback method to return a value as can not return old values
     * as data consistency is very important. Also as mentioned in the config, retries do not occur for client
     * side 4xx errors. Only server side errors.
     * @param accountUid
     * @param weekCommencing
     * @return
     */
    @Retry(name = "starlingApiRetry")
    public StarlingFeedResponse fetchTransactions(String authToken, String accountUid, String maskedAccountUid, LocalDate weekCommencing) {
        log.info("Fetching settled transactions for accountUid: {}, weekCommencing: {}", maskedAccountUid, weekCommencing);
        try {
            String startDate = toStarlingDateFormat(weekCommencing);
            String endDate = toStarlingEndDate(weekCommencing);

            ResponseEntity<StarlingFeedResponse> response = restTemplate.exchange(
                    API_BASE_URL + API_SETTLED_TRANSACTIONS,
                    GET, getHttpEntity(authToken), StarlingFeedResponse.class,
                    accountUid, startDate, endDate);
            if (response.getBody() == null) {
                log.error("Starling API returned null for transaction feed.");
                throw new StarlingApiException("Starling API returned null for transaction feed.");
            }
            log.info("Starling API successfully fetched {} transactions for accountUid: {}", response.getBody().getFeedItems().size(), maskedAccountUid);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Error fetching transactions for accountUid: {} - Status: {}, Response: {}", maskedAccountUid, e.getStatusCode(), e.getResponseBodyAsString());
            throw new StarlingApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error fetching transactions for accountUid: {} - Exception: {}", maskedAccountUid, e.getMessage());
            throw new StarlingApiException("An error occurred when calling Starling API: " + e.getMessage());
        }
    }


}
