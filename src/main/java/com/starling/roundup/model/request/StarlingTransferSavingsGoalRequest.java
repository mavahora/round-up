package com.starling.roundup.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.starling.roundup.model.common.Amount;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StarlingTransferSavingsGoalRequest {

    @JsonProperty("amount")
    private Amount amount ;
}
