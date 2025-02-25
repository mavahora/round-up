package com.starling.roundup.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StarlingBalanceResponse {

    @JsonProperty("clearedBalance")
    private Amount clearedBalance;

    @JsonProperty("effectiveBalance")
    private Amount effectiveBalance;
}
