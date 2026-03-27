package com.stockmarket.repository;

import com.stockmarket.entity.Symbol;
import com.stockmarket.entity.SymbolDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SymbolDetailRepository extends JpaRepository<SymbolDetail, Long> {
    boolean existsBySymbolIdAndExchange(Symbol symbolId, String exchange);

}