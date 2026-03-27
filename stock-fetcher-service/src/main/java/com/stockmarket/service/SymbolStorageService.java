package com.stockmarket.service;

import com.stockmarket.entity.Symbol;
import com.stockmarket.entity.SymbolDetail;
import com.stockmarket.model.SearchResult;
import com.stockmarket.repository.SymbolDetailRepository;
import com.stockmarket.repository.SymbolRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymbolStorageService {
    private final SymbolRepository symbolRepository;
    private final SymbolDetailRepository symbolDetailRepository;

    @Transactional
    public void addSymbol(Map<String, List<SearchResult>> symbolDetails) {

        for (List<SearchResult> results : symbolDetails.values()) {

            if (results.isEmpty()) continue;

            // Extract base symbol ONCE
            String baseSymbol = extractBaseSymbol(results.get(0).getSymbol());

            // Save / fetch symbol
            Symbol symbol = symbolRepository.findBySymbolName(baseSymbol)
                    .orElseGet(() -> {
                        Symbol s = new Symbol();
                        s.setSymbolName(baseSymbol);
                        return symbolRepository.save(s);
                    });

            // Save exchange-specific rows
            for (SearchResult sr : results) {

                boolean exists =
                        symbolDetailRepository.existsBySymbolIdAndExchange(
                                symbol, sr.getExchange());

                if (exists) continue;

                SymbolDetail detail = new SymbolDetail();
                detail.setSymbolId(symbol);
                detail.setName(sr.getName());
                detail.setExchange(sr.getExchange());
                detail.setType(sr.getType());

                symbolDetailRepository.save(detail);
            }
        }
    }

    private String extractBaseSymbol(String symbol) {
        int idx = symbol.lastIndexOf(".");
        return idx == -1 ? symbol : symbol.substring(0, idx);
    }
}
