package com.stockmarket.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/stock-stream")
@RestController
public class TestController {

    @GetMapping("/name")
    public String  getName(){
        return "Stock Stream Service";
    }
}
