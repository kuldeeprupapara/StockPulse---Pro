package com.stockmarket.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum StockExchangeType {
    NSE,
    NSI,
    BSE;

    // Return all enum values as a List
    public static List<StockExchangeType> asList() {
        return Arrays.asList(values());
    }

    // Return all enum names as a List<String>
    public static List<String> namesList() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

}
