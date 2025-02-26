package com.starling.roundup.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starling.roundup.model.common.CurrencyMapping;
import com.starling.roundup.model.common.Amount;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.starling.roundup.util.Constants.GBP;
import static java.math.BigDecimal.TEN;
import static java.math.RoundingMode.HALF_UP;

@Slf4j
public class CurrencyConverter {
    private static Map<String, CurrencyMapping> currencyRates = new HashMap<>();

    static {
        try {
            log.info("Loading currency mapping from currencyRates.json...");
            ObjectMapper mapper = new ObjectMapper();
            currencyRates = mapper.readValue(
                    CurrencyConverter.class.getResourceAsStream("/currencyRates.json"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, CurrencyMapping.class)
            );
            log.info("Successfully loaded currency mappings");
        } catch (Exception e) {
            log.error("Failed to load currency rates.", e);
        }
    }

    public static long convertToGBP(String requestId, Amount amount) {
        String currency = amount.getCurrency();
        long minorUnits = amount.getMinorUnits();
        log.debug("RequestId: {}, converting {} minor units from {} to GBP...", requestId, minorUnits, currency);
        CurrencyMapping currencyMapping = currencyRates.get(currency);
        if (currencyMapping == null) {
            // If no conversion exists, return 0 which will ignore this transaction as currency is not supported
            log.warn("RequestId: {}, Currency not supported: {}. Skipping round-up.", requestId, currency);
            return 0L;
        }

        // Convert minor units to the real currency amount
        BigDecimal decimalCurrencyAmount = new BigDecimal(minorUnits).divide(TEN.pow(currencyMapping.getDecimalPlaces()), 10, HALF_UP);
        // Convert real currency to GBP
        BigDecimal gbpAmount = decimalCurrencyAmount.multiply(currencyMapping.getConversionRateToGBP());
        // Convert the GBP amount to minor units (pence)
        long gbpMinorUnitsAmount = gbpAmount.multiply(new BigDecimal(100)).setScale(0, HALF_UP).longValue();
        log.info("RequestId: {}, Converted {} to {} GBP minor units.", requestId, minorUnits, gbpMinorUnitsAmount);

        return gbpMinorUnitsAmount;
    }
}