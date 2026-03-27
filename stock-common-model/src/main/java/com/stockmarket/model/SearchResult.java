package com.stockmarket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchResult{
    private String symbol;
    private String name;
    private String exchange;
    private String type;
}