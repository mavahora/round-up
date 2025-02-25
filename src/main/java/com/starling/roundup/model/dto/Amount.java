package com.starling.roundup.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class Amount {
    private String currency;

    @JsonProperty("minorUnits")
    private long minorUnits;

    @JsonIgnore
    public BigDecimal getAmountInPounds() {
        return BigDecimal.valueOf(minorUnits).divide(BigDecimal.valueOf(100)); // Convert to pounds
    }
}
