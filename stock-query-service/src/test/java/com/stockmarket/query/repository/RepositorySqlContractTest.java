package com.stockmarket.query.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class RepositorySqlContractTest {

    @Test
    void indexConstituentQueriesMustJoinMasterExchange() throws Exception {
        assertQueryContains(IndexConstituentRepository.class, "getConstituentSymbolsForIndex", "master_exchange");
        assertQueryContains(IndexConstituentRepository.class, "getTopConstituentsByMarketCap", "master_exchange");
    }

    @Test
    void symbolRepositoryQueriesMustJoinMasterExchange() throws Exception {
        assertQueryContains(SymbolRepository.class, "getSymbol", "master_exchange");
        assertQueryContains(SymbolRepository.class, "getAllSymbolsFromDetails", "master_exchange");
    }

    private void assertQueryContains(Class<?> repositoryClass, String methodName, String requiredSqlToken) throws Exception {
        Method method = Arrays.stream(repositoryClass.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Method not found: " + methodName));

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "@Query annotation missing for " + repositoryClass.getSimpleName() + "." + methodName);
        assertTrue(
                query.value().toLowerCase().contains(requiredSqlToken.toLowerCase()),
                "Expected SQL token '" + requiredSqlToken + "' in " + repositoryClass.getSimpleName() + "." + methodName
        );
    }
}

