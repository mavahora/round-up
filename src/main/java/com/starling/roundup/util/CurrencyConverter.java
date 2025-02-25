package com.starling.roundup.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starling.roundup.model.CurrencyMapping;
import com.starling.roundup.model.dto.Amount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static com.starling.roundup.util.Constants.GBP;
import static java.math.BigDecimal.TEN;
import static java.math.RoundingMode.*;

public class CurrencyConverter {
    private static Map<String, CurrencyMapping> currencyRates = new HashMap<>();

    static {
        try {
            // Load currency rates from a JSON file
            ObjectMapper mapper = new ObjectMapper();
            currencyRates = mapper.readValue(
                    CurrencyConverter.class.getResourceAsStream("/currencyRates.json"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, CurrencyMapping.class)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long convertToGBP(Amount amount) {
        // If currency is GBP, then no need to convert
        if(GBP.equals(amount.getCurrency())) return amount.getMinorUnits();

        CurrencyMapping currencyMapping = currencyRates.get(amount.getCurrency());
        if (currencyMapping == null) {
            // If no conversion exists, return 0 which will ignore this transaction as currency is not supported
            return 0L;
        }

        // Transaction amount in minor units, other than GBP
        BigDecimal minorUnits = new BigDecimal(amount.getMinorUnits());

        // Convert minor units to the real currency amount
        BigDecimal realCurrencyAmount = minorUnits.divide(TEN.pow(currencyMapping.getDecimalPlaces()), 10, HALF_UP);
        // Convert real currency to GBP
        BigDecimal gbpAmount = realCurrencyAmount.multiply(currencyMapping.getConversionRateToGBP());
        // Convert the GBP amount to minor units (pence)
        return gbpAmount.multiply(new BigDecimal(100)).setScale(0, HALF_UP).longValue();
    }
}