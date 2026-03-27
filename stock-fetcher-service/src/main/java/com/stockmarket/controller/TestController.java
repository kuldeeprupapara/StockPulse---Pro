package com.stockmarket.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-fetcher")
public class TestController {

    @GetMapping("/name")
    public String getName(){
        return "Stock Fetcher Service";
    }
}
