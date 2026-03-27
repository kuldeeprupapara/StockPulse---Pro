package com.stockmarket.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class MarketDataProviderConfig {

    @Value("${market.data.provider.base-url}")
    private String baseUrl;

    @Value("${market.data.provider.quote-endpoint}")
    private String quoteEndpoint;

    @Value("${market.data.provider.chart.endpoint}")
    private String chartEndpoint;

    @Value("${market.data.provider.search.endpoint}")
    private String searchEndPoint;

    @Value("${market.data.provider.company.summary.endpoint}")
    public String companySummaryEndpoint;

    @Value("${market.data.provider.crumb-endpoint}")
    private String crumbEndpoint;

    @Value("${market.data.provider.market.mover.endpoint}")
    private String marketMoverEndpoint;

    @Value("${market.data.provider.cookie-urls}")
    private List<String> cookieUrls;

    @Value("${market.data.provider.timeout-seconds}")
    private int timeoutSeconds;

    @Value("${market.data.provider.crumb-refresh-interval-minutes}")
    private int crumbRefreshIntervalMinutes;

    @Value("${market.data.provider.max-retries}")
    private int maxRetries;

    @Value("${market.data.provider.user-agent}")
    private String userAgent;

    @Value("${market.data.provider.accept-header}")
    private String acceptHeader;

    @Value("${market.data.provider.accept-language}")
    private String acceptLanguage;

    @Value("${market.data.provider.referer}")
    private String referer;

    public long getCrumbRefreshIntervalMillis() {
        return crumbRefreshIntervalMinutes * 60 * 1000L;
    }

    public int getTimeoutMillis() {
        return timeoutSeconds * 1000;
    }

}
