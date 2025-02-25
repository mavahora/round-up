package com.starling.roundup.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StarlingTransferSavingsGoalResponse {

    @JsonProperty("transferUid")
    private String transferUid ;

    @JsonProperty("success")
    private boolean success;
}
