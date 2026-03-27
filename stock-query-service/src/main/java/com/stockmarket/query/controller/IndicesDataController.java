package com.stockmarket.query.controller;

import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.query.service.IndicesDataService;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/query")
@RequiredArgsConstructor
public class IndicesDataController {

    private final IndicesDataService indicesDataService;

    @GetMapping("/load-indice-data")
    public void loadIndicesData(){
        indicesDataService.preFetchIndexData();
    }

    @PostMapping("/store-index-constituents")
    public void storeIndexConstituents(@RequestBody Map<String, Integer> indexMap){
        indicesDataService.storeIndexConstituents(indexMap);
    }

    @PostMapping("/store-index-data")
    public APIRespDTO<Object> storeIndexData(@RequestParam(required = false) Integer countryId){
        return indicesDataService.storeIndexData(countryId);
    }

    @PostMapping("/store-index-data/{indexId}")
    public APIRespDTO<Object> storeIndexDataByIndexId(
            @PathVariable Integer indexId,
            @RequestParam String exchange
    ) {
        return indicesDataService.storeIndexConstituentData(indexId, exchange);
    }

    @GetMapping("/get-indexes-data/{countryId}")
    public APIRespDTO<Object> getIndexesData(@PathVariable Integer countryId){
        return indicesDataService.getIndexData(countryId);
    }

    @GetMapping("/constituents/{indexid}")
    public APIRespDTO<Object> getConstituents(
            @PathVariable("indexid") Integer indexId,
            @RequestParam String exchange
    ) {
        return indicesDataService.getConstituents(indexId, exchange);
    }

    @GetMapping("/aggregates/index-ohlc")
    public APIRespDTO<Object> getIndexOhlc(
            @RequestParam Integer indexId,
            @RequestParam(required = false) OffsetDateTime fromTs,
            @RequestParam(required = false) OffsetDateTime toTs,
            @RequestParam(defaultValue = "5") Integer bucketMinutes
    ) {
        return indicesDataService.getIndexOhlc(indexId, fromTs, toTs, bucketMinutes);
    }
}
