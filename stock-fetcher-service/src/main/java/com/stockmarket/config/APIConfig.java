package com.stockmarket.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class APIConfig {
    private String endPoint;
    private boolean enableForAPICall;
}
