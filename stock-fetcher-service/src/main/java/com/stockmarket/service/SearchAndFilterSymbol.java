package com.stockmarket.service;

import com.stockmarket.client.DefaultMarketClient;
import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.model.SearchResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAndFilterSymbol {
    private final SymbolIngestionService symbolIngestionService;
    private final DefaultMarketClient defaultMarketClient;
    private final SymbolStorageService symbolStorageService;

    public APIRespDTO<Object> search(){

        final List<String>securities = symbolIngestionService.getSecurities();

        final Map<String,List<SearchResult>> result = new ConcurrentHashMap<>();
        final List<String> failedSymbols = new CopyOnWriteArrayList<>();

        int BATCH_SIZE = 40;
        final ExecutorService executor = Executors.newFixedThreadPool(BATCH_SIZE);
        AtomicInteger counter = new AtomicInteger(0);

        try{
            // THREAD CONFIG
            int MAX_CONCURRENCY = 30;
            for(int i = 0; i<securities.size(); i+= MAX_CONCURRENCY){
                int end = Math.min(i+ MAX_CONCURRENCY,securities.size());
                List<String> batch = securities.subList(i,end);

                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for(String symbol: batch){
                    CompletableFuture<Void> future = CompletableFuture.runAsync(()->{
                        try{
                            List<SearchResult> searchResultList = defaultMarketClient.search(symbol);
                            if(searchResultList.isEmpty()){
                                failedSymbols.add(symbol);
                            }else{
                                result.put(symbol,searchResultList);
                            }

                            int processed = counter.incrementAndGet();
                            if(processed%50==0){
                                log.info("Progress: {}/{} symbols processed ({}%)",
                                        processed, securities.size(),
                                        Math.round((processed * 100.0) / securities.size()));
                            }
                        }catch (Exception e){
                            failedSymbols.add(symbol);
                            log.error("Error searching symbol: {}",symbol,e);
                        }
                    },executor);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                if(end < securities.size()){
                    Thread.sleep(1000);
                }
            }
            log.info("Search completed. Success: {}, Failed: {}",
                    result.size(), failedSymbols.size());

            Map<String,List<SearchResult>> orderedResult = new LinkedHashMap<>(result);

            log.info("Starting database storage...");
            long storageStartTime = System.currentTimeMillis();
            symbolStorageService.addSymbol(orderedResult);
            long storageDuration = System.currentTimeMillis() - storageStartTime;
            log.info("Storage completed in {}ms", storageDuration);

            return  APIRespDTO.builder()
                    .status(1)
                    .data(Map.of(
                            "result", orderedResult,
                            "failed", failedSymbols,
                            "total", securities.size(),
                            "success", result.size(),
                            "failedCount", failedSymbols.size()
                    ))
                    .build();
        }catch (Exception e){
            log.error("Error during batch processing", e);
            throw new RuntimeException("Failed to process symbols", e);
        }finally {
            executor.shutdown();

            try{
                if(!executor.awaitTermination(60, TimeUnit.SECONDS)){
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}



//        final List<String> securities = symbolIngestionService.getSecurities();
//        Map<String, SearchResult> result = new LinkedHashMap<>();
//        List<String> failedSecurities = new ArrayList<>();
//        for (String symbol : securities) {
//            List<SearchResult> search = defaultMarketClient.search(symbol);
//            if (search.isEmpty()) {
//                failedSecurities.add(symbol);
//            }else{
//                result.put(symbol, search.get(0));
//            }
//        }
