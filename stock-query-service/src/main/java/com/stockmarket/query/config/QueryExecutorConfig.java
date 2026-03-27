package com.stockmarket.query.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryExecutorConfig {

    @Bean(name = "marketOverviewExecutor", destroyMethod = "shutdown")
    public ExecutorService marketOverviewExecutor() {
        return Executors.newFixedThreadPool(15);
    }
}

