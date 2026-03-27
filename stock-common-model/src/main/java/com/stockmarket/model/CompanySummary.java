package com.stockmarket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanySummary{
    private String symbol;
    private String companyName;
    private String sector;
    private String industry;
    private String businessSummary;
    private String address;
    private double marketCap;
    private double peRatio;
    private double week52High;
    private double week52Low;
}
