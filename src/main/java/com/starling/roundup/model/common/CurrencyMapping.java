package com.starling.roundup.model.common;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CurrencyMapping {
    private BigDecimal conversionRateToGBP;
    private int decimalPlaces; // Decimal places the currency has

}