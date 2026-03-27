package com.stockmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource(value = "classpath:configuration.properties")
public class StockFetcherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockFetcherServiceApplication.class, args);
	}

}
