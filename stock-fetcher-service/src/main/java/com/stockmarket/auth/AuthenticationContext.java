package com.stockmarket.auth;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class AuthenticationContext {
    private Map<String, String> cookies;
    private String crumb;
    private long timestamp;

    public boolean isValid(long refreshIntervalMillis) {
        return cookies != null
                && !cookies.isEmpty()
                && (System.currentTimeMillis() - timestamp) < refreshIntervalMillis;
    }
}
