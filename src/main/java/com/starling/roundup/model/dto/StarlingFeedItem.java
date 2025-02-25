package com.starling.roundup.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StarlingFeedItem {

    @JsonProperty("feedItemUid")
    private String transactionId;

    @JsonProperty("amount")
    private Amount amount;

    @JsonProperty("direction")
    private TransactionDirection direction;

    @JsonProperty("transactionTime")
    private ZonedDateTime transactionTime;
}
