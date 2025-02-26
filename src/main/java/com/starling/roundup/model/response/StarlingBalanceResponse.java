package com.starling.roundup.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.starling.roundup.model.common.Amount;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StarlingBalanceResponse {

    // Only interested in the effective balance as this includes both the
    // cleared balance and any pending transactions
    @JsonProperty("effectiveBalance")
    private Amount effectiveBalance;
}
