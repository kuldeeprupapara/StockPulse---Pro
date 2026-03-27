package com.stockmarket.client;

import com.stockmarket.auth.AuthenticationContext;
import com.stockmarket.auth.MarketDataAuthenticationService;
import com.stockmarket.config.MarketDataProviderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClientHelper {

    private final MarketDataProviderConfig config;
    private final MarketDataAuthenticationService authService;

    public String executeGet(String url) throws IOException {
        return executeGet(url, new HashMap<>());
    }

    public String executeGet(String url, Map<String, String> customHeaders) throws IOException {
        AuthenticationContext authContext = authService.authContext();

        Connection connection = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(config.getUserAgent())
                .cookies(authContext.getCookies())
                .header("Accept", config.getAcceptHeader())
                .header("Accept-Language", config.getAcceptLanguage())
                .header("Referer", config.getReferer())
                .header("Origin", extractOrigin(config.getReferer()))
                .timeout(config.getTimeoutMillis());

        for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
            connection.header(entry.getKey(), entry.getValue());
        }

        try {
            log.debug("Executing GET request to: {}", url);
            return connection.execute().body();
        } catch (IOException e) {
            log.error("HTTP GET failed for URL: {}", url, e);
            throw e;
        }
    }

    /**
     * Build URL from base endpoint and optional path variable + query params + crumb.
     *
     * Example:
     *  buildUrlWithAuth("/v8/finance/chart", "AAPL", Map.of("interval","1m","range","1d"))
     *  -> https://.../v8/finance/chart/AAPL?interval=1m&range=1d&crumb=...
     *
     *  buildUrlWithAuth("/v7/finance/quote", null, Map.of("symbols","AAPL,MSFT"))
     *  -> https://.../v7/finance/quote?symbols=AAPL,MSFT&crumb=...
     */
    public String buildUrlWithAuth(String baseEndpoint,
                                   String pathVariable,
                                   Map<String, String> queryParams) throws IOException {
        AuthenticationContext authContext = authService.authContext();

        StringBuilder url = new StringBuilder(config.getBaseUrl())
                .append(baseEndpoint);

        if (pathVariable != null && !pathVariable.isEmpty()) {
            if (!baseEndpoint.endsWith("/")) {
                url.append("/");
            }
            // IMPORTANT: encode path variable (handles ^GSPC -> %5EGSPC)
            url.append(encode(pathVariable));
        }

        boolean hasQuery = false;

        if (queryParams != null && !queryParams.isEmpty()) {
            url.append("?");
            hasQuery = true;

            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                url.append(encode(entry.getKey()))
                        .append("=")
                        .append(encode(entry.getValue()))
                        .append("&");
            }
        }

        if (authContext.getCrumb() != null && !authContext.getCrumb().isEmpty()) {
            if (!hasQuery) {
                url.append("?");
            } else if (url.charAt(url.length() - 1) != '&'
                    && url.charAt(url.length() - 1) != '?') {
                url.append("&");
            }
            url.append("crumb=").append(encode(authContext.getCrumb()));
        } else if (hasQuery && url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }

        return url.toString();
    }

    /**
     * Backward-compatible method if you don't need path variable.
     */
    public String buildUrlWithAuth(String baseEndpoint, Map<String, String> queryParams) throws IOException {
        return buildUrlWithAuth(baseEndpoint, null, queryParams);
    }

    public String executeGetWithRetry(String url, int maxRetries) throws IOException {
        int attempt = 0;
        IOException lastException = null;

        while (attempt < maxRetries) {
            try {
                return executeGet(url);
            } catch (IOException e) {
                lastException = e;
                String errorMsg = e.getMessage();

                boolean unauthorized = errorMsg != null &&
                        (errorMsg.contains("401") || errorMsg.toLowerCase().contains("unauthorized"));

                if (unauthorized && attempt + 1 < maxRetries) {
                    attempt++;
                    log.warn("Unauthorized error on attempt {}, refreshing auth...", attempt);
                    authService.refreshAuthContext();
                } else {
                    throw e;
                }
            }
        }

        throw lastException != null ? lastException : new IOException("Max retries exceeded");
    }

    private String extractOrigin(String referer) {
        if (referer == null || referer.isEmpty()) {
            return "";
        }
        try {
            int endIndex = referer.indexOf("/", 8); // after "https://"
            return endIndex > 0 ? referer.substring(0, endIndex) : referer;
        } catch (Exception e) {
            return referer;
        }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
