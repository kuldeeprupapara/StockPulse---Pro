package com.stockmarket.client;

import com.stockmarket.model.SearchResult;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FetcherSearchClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String fetcherUrl;

    public FetcherSearchClient(@Value("${stock.fetcher.url:http://localhost:8081}") String fetcherUrl) {
        this.fetcherUrl = fetcherUrl;
    }

    public List<SearchResult> search(String query, String countryCode, String exchange) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(fetcherUrl)
                .path("/internal/market/search")
                .queryParam("q", query);

        if (countryCode != null && !countryCode.isBlank()) {
            builder.queryParam("countryCode", countryCode);
        }
        if (exchange != null && !exchange.isBlank()) {
            builder.queryParam("exchange", exchange);
        }

        String url = builder.toUriString();

        ResponseEntity<List<SearchResult>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SearchResult>>() {}
        );

        return response.getBody() == null ? List.of() : response.getBody();
    }
}

