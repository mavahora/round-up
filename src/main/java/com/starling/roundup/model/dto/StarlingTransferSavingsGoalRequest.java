package com.starling.roundup.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
