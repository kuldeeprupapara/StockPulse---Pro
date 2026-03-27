package com.stockmarket.repository;

import com.stockmarket.entity.Symbol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, Integer> {
    Optional<Symbol> findBySymbolName(String symbolName);
}